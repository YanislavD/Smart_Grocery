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
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebAuthController {
    private static final int EXPIRY_WARNING_DAYS = 7;

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
        model.addAttribute("usersCount", userService.countUsers());
        model.addAttribute("productsCount", productService.countProducts());
        model.addAttribute("pantryItemsCount", pantryService.countPantryItemsByHousehold(household.getId()));
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
        populateHouseholdModel(model, context.get().user(), context.get().household());
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
            HouseholdInviteView invite = toView(householdInviteService.createInvite(
                    context.get().household().getId(),
                    context.get().user().getId(),
                    email
            ));
            return "redirect:/household?invitedToken=" + invite.token();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("inviteError", ex.getMessage());
            populateHouseholdModel(model, context.get().user(), context.get().household());
            return "household";
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
        return Optional.of(new DashboardContext(user, household));
    }

    private record DashboardContext(User user, Household household) {
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

    private void populateHouseholdModel(Model model, User user, Household household) {
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
