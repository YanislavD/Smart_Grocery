package projects.smart_grocery.web;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import projects.smart_grocery.auth.AuthService;
import projects.smart_grocery.auth.dto.RegisterRequest;
import projects.smart_grocery.household.Household;
import projects.smart_grocery.household.HouseholdInviteService;
import projects.smart_grocery.household.HouseholdInviteView;
import projects.smart_grocery.household.IncomingHouseholdInviteView;
import projects.smart_grocery.household.HouseholdMember;
import projects.smart_grocery.household.HouseholdMemberView;
import projects.smart_grocery.household.HouseholdMemberService;
import projects.smart_grocery.household.HouseholdService;
import projects.smart_grocery.pantry.PantryItem;
import projects.smart_grocery.pantry.PantryService;
import projects.smart_grocery.pantry.Product;
import projects.smart_grocery.pantry.ProductService;
import projects.smart_grocery.pantry.UnitType;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.pantry.dto.UpdatePantryItemRequest;
import projects.smart_grocery.shopping.ShoppingListService;
import projects.smart_grocery.shopping.ShoppingListViewItem;
import projects.smart_grocery.user.User;
import projects.smart_grocery.user.UserService;
import projects.smart_grocery.web.dto.PantryItemForm;
import projects.smart_grocery.web.dto.RegisterForm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.security.Principal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebAuthController {
    private static final int EXPIRY_WARNING_DAYS = 7;
    private static final int DASHBOARD_EXPIRY_DAYS = 5;

    private final AuthService authService;
    private final UserService userService;
    private final ProductService productService;
    private final PantryService pantryService;
    private final ShoppingListService shoppingListService;
    private final HouseholdService householdService;
    private final HouseholdMemberService householdMemberService;
    private final HouseholdInviteService householdInviteService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @GetMapping("/")
    public String homePage(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        if (principal == null) {
            return "redirect:/login";
        }

        if (userService.findByEmail(principal.getName()).isEmpty()) {
            forceLogout(request, response);
            return "redirect:/login?sessionReset";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            authService.register(new RegisterRequest(form.getEmail(), form.getPassword()));
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registerError", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Principal principal,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        User user = context.get().user();
        Household household = context.get().household();

        model.addAttribute("email", user.getEmail());
        model.addAttribute("householdName", household.getName());
        List<PantryItem> householdItems = pantryService.getByHousehold(household.getId());
        model.addAttribute("dashboardTotalItemsCount", householdItems.size());
        BigDecimal totalUnits = householdItems.stream()
                .map(PantryItem::getQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("dashboardTotalUnitsText", totalUnits.stripTrailingZeros().toPlainString());
        long expiringIn3DaysCount = householdItems.stream()
                .filter(item -> item.getExpiryDate() != null)
                .map(item -> ChronoUnit.DAYS.between(LocalDate.now(), item.getExpiryDate()))
                .filter(days -> days >= 0 && days <= 3)
                .count();
        model.addAttribute("dashboardExpiringSoonCount", expiringIn3DaysCount);
        long toRestockCount = householdItems.stream().filter(pantryService::isLowStock).count();
        model.addAttribute("dashboardToRestockCount", toRestockCount);
        List<ExpiringItemView> expiringItems = householdItems.stream()
                .filter(item -> item.getExpiryDate() != null)
                .map(item -> {
                    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpiryDate());
                    return new ExpiringItemView(
                            item.getProduct().getName(),
                            item.getProduct().getCategory() == null ? "Uncategorized" : item.getProduct().getCategory(),
                            formatQty(item.getQty(), item.getUnit().name()),
                            daysUntil
                    );
                })
                .filter(item -> item.daysUntil() >= 0 && item.daysUntil() <= DASHBOARD_EXPIRY_DAYS)
                .sorted((a, b) -> Long.compare(a.daysUntil(), b.daysUntil()))
                .toList();
        model.addAttribute("expiringItems", expiringItems);

        LinkedHashMap<String, Long> categoryCounts = householdItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory() == null || item.getProduct().getCategory().isBlank()
                                ? "Uncategorized"
                                : item.getProduct().getCategory(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        long maxCategoryCount = categoryCounts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        List<CategorySummaryView> categorySummary = categoryCounts.entrySet().stream()
                .map(entry -> new CategorySummaryView(
                        entry.getKey(),
                        entry.getValue(),
                        (int) Math.max(8, Math.round((entry.getValue() * 100.0) / maxCategoryCount))
                ))
                .toList();
        model.addAttribute("categorySummary", categorySummary);
        return "dashboard";
    }

    @GetMapping("/pantry")
    public String pantryPage(Principal principal,
                             @RequestParam(defaultValue = "false") boolean lowStockOnly,
                             @RequestParam(defaultValue = "false") boolean expiringSoonOnly,
                             @RequestParam(required = false) String category,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(defaultValue = "expiryDate") String sort,
                             @RequestParam(defaultValue = "asc") String dir,
                             Model model,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        String mappedSortField = mapPantrySortField(sort);
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, mappedSortField));
        String selectedCategory = normalizeCategory(category);
        Page<PantryItem> pantryPage = pantryService.getByHouseholdPaged(
                household.getId(),
                lowStockOnly,
                expiringSoonOnly,
                EXPIRY_WARNING_DAYS,
                selectedCategory,
                pageable
        );
        List<PantryItem> items = pantryPage.getContent();

        if (!model.containsAttribute("pantryItemForm")) {
            PantryItemForm form = new PantryItemForm();
            form.setUnit(UnitType.PCS);
            form.setMinQtyThreshold(java.math.BigDecimal.ONE);
            model.addAttribute("pantryItemForm", form);
        }

        populatePantryModel(model, context.get().user(), household, items);
        model.addAttribute("lowStockOnly", lowStockOnly);
        model.addAttribute("expiringSoonOnly", expiringSoonOnly);
        model.addAttribute("allItemsCount", pantryService.countPantryItemsByHousehold(household.getId()));
        model.addAttribute("filteredItemsCount", pantryPage.getTotalElements());
        model.addAttribute("currentPage", pantryPage.getNumber());
        model.addAttribute("totalPages", pantryPage.getTotalPages());
        model.addAttribute("pageSize", pantryPage.getSize());
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", direction.name().toLowerCase());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("selectedCategory", selectedCategory == null ? "" : selectedCategory);
        model.addAttribute("categories", productService.findDistinctCategories());
        return "pantry";
    }

    @PostMapping("/pantry")
    public String addPantryItem(Principal principal,
                                @Valid @ModelAttribute("pantryItemForm") PantryItemForm form,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        if (bindingResult.hasErrors()) {
            populatePantryModel(model, context.get().user(), household, pantryService.getByHousehold(household.getId()));
            return "pantry";
        }

        try {
            pantryService.create(new CreatePantryItemRequest(
                    household.getId(),
                    form.getProductId(),
                    form.getQty(),
                    form.getUnit(),
                    form.getExpiryDate(),
                    form.getMinQtyThreshold()
            ));
            return "redirect:/pantry";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pantryError", ex.getMessage());
            populatePantryModel(model, context.get().user(), household, pantryService.getByHousehold(household.getId()));
            return "pantry";
        }
    }

    @PostMapping("/pantry/update")
    public String updatePantryItem(@RequestParam Long id,
                                   Principal principal,
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                   BigDecimal qty,
                                   UnitType unit,
                                   BigDecimal minQtyThreshold,
                                   Model model,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        try {
            pantryService.updateForHousehold(
                    id,
                    household.getId(),
                    new UpdatePantryItemRequest(qty, unit, expiryDate, minQtyThreshold)
            );
            return "redirect:/pantry";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pantryError", ex.getMessage());
            populatePantryModel(model, context.get().user(), household, pantryService.getByHousehold(household.getId()));
            return "pantry";
        }
    }

    @PostMapping("/pantry/{id}/delete")
    public String deletePantryItem(@PathVariable Long id,
                                   Principal principal,
                                   Model model,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        try {
            pantryService.deleteForHousehold(id, household.getId());
            return "redirect:/pantry";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("pantryError", ex.getMessage());
            populatePantryModel(model, context.get().user(), household, pantryService.getByHousehold(household.getId()));
            return "pantry";
        }
    }

    @GetMapping("/shopping-list")
    public String shoppingListPage(Principal principal,
                                   Model model,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        List<ShoppingListViewItem> items = shoppingListService.getViewItemsForHousehold(household.getId());
        model.addAttribute("email", context.get().user().getEmail());
        model.addAttribute("householdName", household.getName());
        model.addAttribute("shoppingItems", items);
        model.addAttribute("checkedCount", items.stream().filter(ShoppingListViewItem::checked).count());
        return "shopping-list";
    }

    @PostMapping("/shopping-list/generate")
    public String generateShoppingList(Principal principal,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        shoppingListService.generateFromLowStock(context.get().household().getId());
        return "redirect:/shopping-list?generated";
    }

    @PostMapping("/shopping-list/items/{id}/qty")
    public String updateShoppingListItemQty(@PathVariable Long id,
                                            @RequestParam BigDecimal qty,
                                            Principal principal,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        shoppingListService.updateItemQty(context.get().household().getId(), id, qty);
        return "redirect:/shopping-list";
    }

    @PostMapping("/shopping-list/items/{id}/check")
    public String checkShoppingListItem(@PathVariable Long id,
                                        @RequestParam(defaultValue = "false") boolean checked,
                                        Principal principal,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        shoppingListService.markItemChecked(context.get().household().getId(), id, checked);
        return "redirect:/shopping-list";
    }

    @PostMapping("/shopping-list/items/{id}/delete")
    public String removeShoppingListItem(@PathVariable Long id,
                                         Principal principal,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        shoppingListService.removeItem(context.get().household().getId(), id);
        return "redirect:/shopping-list";
    }

    @GetMapping("/household")
    public String householdPage(Principal principal,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        populateHouseholdModel(model, context.get().user(), context.get().household(), context.get().membership());
        return "household";
    }

    @PostMapping("/household/invites")
    public String inviteMember(@RequestParam String email,
                               Principal principal,
                               Model model,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            ensureOwner(context.get().membership());
            HouseholdInviteView invite = toView(householdInviteService.createInvite(
                    context.get().household().getId(),
                    context.get().user().getId(),
                    email
            ));
            return "redirect:/household?invitedToken=" + invite.token();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("inviteError", ex.getMessage());
            populateHouseholdModel(model, context.get().user(), context.get().household(), context.get().membership());
            return "household";
        }
    }

    @PostMapping("/household/settings/name")
    public String updateHouseholdName(@RequestParam String householdName,
                                      Principal principal,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            ensureOwner(context.get().membership());
            householdService.renameHousehold(context.get().household().getId(), householdName);
            return "redirect:/household?settingsSaved";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?settingsError";
        }
    }

    @PostMapping("/household/settings/members/{userId}/role")
    public String updateMemberRole(@PathVariable Long userId,
                                   @RequestParam String role,
                                   Principal principal,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            ensureOwner(context.get().membership());
            householdMemberService.changeRole(context.get().household().getId(), userId, role);
            return "redirect:/household?settingsSaved";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?settingsError";
        }
    }

    @PostMapping("/household/settings/members/{userId}/remove")
    public String removeMember(@PathVariable Long userId,
                               Principal principal,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            ensureOwner(context.get().membership());
            householdMemberService.removeMember(context.get().household().getId(), userId);
            return "redirect:/household?settingsSaved";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?settingsError";
        }
    }

    @PostMapping("/household/settings/invites/{inviteId}/cancel")
    public String cancelInvite(@PathVariable Long inviteId,
                               Principal principal,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            ensureOwner(context.get().membership());
            householdInviteService.cancelInvite(context.get().household().getId(), inviteId);
            return "redirect:/household?settingsSaved";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?settingsError";
        }
    }

    @GetMapping("/household/invites/accept")
    public String acceptInvite(@RequestParam String token,
                               Principal principal,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            householdInviteService.acceptInvite(token, context.get().user().getId());
            return "redirect:/household?inviteAccepted";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?inviteError";
        }
    }

    @PostMapping("/household/invites/{token}/accept")
    public String acceptInviteFromList(@PathVariable String token,
                                       Principal principal,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        try {
            householdInviteService.acceptInvite(token, context.get().user().getId());
            return "redirect:/household?inviteAccepted";
        } catch (IllegalArgumentException ex) {
            return "redirect:/household?inviteError";
        }
    }

    private void forceLogout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
    }

    private Optional<DashboardContext> resolveDashboardContext(Principal principal,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        if (principal == null) {
            return Optional.empty();
        }

        Optional<User> userOptional = userService.findByEmail(principal.getName());
        if (userOptional.isEmpty()) {
            forceLogout(request, response);
            return Optional.empty();
        }

        User user = userOptional.get();
        HouseholdMember membership = householdMemberService.findPrimaryMembershipByUserId(user.getId())
                .orElseGet(() -> {
                    Household newHousehold = householdService.createDefaultHouseholdForUser(user.getId(), user.getEmail());
                    return householdMemberService.addOwnerMembership(newHousehold.getId(), user.getId());
                });
        Household household = householdService.getByIdOrThrow(membership.getHouseholdId());
        return Optional.of(new DashboardContext(user, household, membership));
    }

    private record DashboardContext(User user, Household household, HouseholdMember membership) {
    }

    private String mapPantrySortField(String sortField) {
        return switch (sortField) {
            case "product" -> "product.name";
            case "qty" -> "qty";
            case "unit" -> "unit";
            case "minQtyThreshold" -> "minQtyThreshold";
            case "expiryDate" -> "expiryDate";
            default -> "expiryDate";
        };
    }

    private HouseholdInviteView toView(projects.smart_grocery.household.HouseholdInvite invite) {
        return new HouseholdInviteView(invite.getId(), invite.getEmail(), invite.getStatus(), invite.getToken(), invite.getCreatedAt());
    }

    private void populateHouseholdModel(Model model, User user, Household household, HouseholdMember currentMembership) {
        List<HouseholdMemberView> members = householdMemberService.getByHouseholdId(household.getId()).stream()
                .map(member -> new HouseholdMemberView(
                        member.getUserId(),
                        userService.getByIdOrThrow(member.getUserId()).getEmail(),
                        member.getRole()
                ))
                .toList();

        model.addAttribute("email", user.getEmail());
        model.addAttribute("householdName", household.getName());
        model.addAttribute("members", members);
        model.addAttribute("invites", householdInviteService.getInvitesForHousehold(household.getId()));
        List<IncomingHouseholdInviteView> incomingInvites =
                householdInviteService.getIncomingPendingInvitesForEmail(user.getEmail());
        model.addAttribute("incomingInvites", incomingInvites);
        model.addAttribute("isOwner", "OWNER".equalsIgnoreCase(currentMembership.getRole()));
        model.addAttribute("currentUserId", user.getId());
    }

    private void ensureOwner(HouseholdMember membership) {
        if (!"OWNER".equalsIgnoreCase(membership.getRole())) {
            throw new IllegalArgumentException("Only owners can manage household settings");
        }
    }

    private String formatQty(BigDecimal qty, String unit) {
        if (qty == null) {
            return "- " + unit;
        }
        return qty.stripTrailingZeros().toPlainString() + " " + unit;
    }

    private record ExpiringItemView(String productName, String category, String qtyText, long daysUntil) {
    }

    private record CategorySummaryView(String category, long count, int percentage) {
    }

    private void populatePantryModel(Model model, User user, Household household, List<PantryItem> items) {
        Set<Long> lowStockItemIds = items.stream()
                .filter(pantryService::isLowStock)
                .map(PantryItem::getId)
                .collect(Collectors.toSet());
        Set<Long> expiringSoonItemIds = items.stream()
                .filter(item -> pantryService.isExpiringSoon(item, EXPIRY_WARNING_DAYS))
                .map(PantryItem::getId)
                .collect(Collectors.toSet());
        Set<Long> expiredItemIds = items.stream()
                .filter(pantryService::isExpired)
                .map(PantryItem::getId)
                .collect(Collectors.toSet());

        model.addAttribute("email", user.getEmail());
        model.addAttribute("householdName", household.getName());
        model.addAttribute("pantryItems", items);
        model.addAttribute("products", productService.findAllProducts());
        model.addAttribute("unitTypes", UnitType.values());
        model.addAttribute("lowStockItemIds", lowStockItemIds);
        model.addAttribute("expiringSoonItemIds", expiringSoonItemIds);
        model.addAttribute("expiredItemIds", expiredItemIds);
        model.addAttribute("lowStockOnly", false);
        model.addAttribute("expiringSoonOnly", false);
        model.addAttribute("allItemsCount", items.size());
        model.addAttribute("filteredItemsCount", items.size());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("pageSize", 20);
        model.addAttribute("sortField", "expiryDate");
        model.addAttribute("sortDir", "asc");
        model.addAttribute("reverseSortDir", "desc");
        model.addAttribute("selectedCategory", "");
        model.addAttribute("categories", productService.findDistinctCategories());
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String normalized = category.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
