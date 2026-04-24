package projects.smart_grocery.web;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import projects.smart_grocery.auth.AuthService;
import projects.smart_grocery.auth.dto.RegisterRequest;
import projects.smart_grocery.pantry.Household;
import projects.smart_grocery.pantry.HouseholdMember;
import projects.smart_grocery.pantry.HouseholdMemberService;
import projects.smart_grocery.pantry.HouseholdService;
import projects.smart_grocery.pantry.PantryItem;
import projects.smart_grocery.pantry.PantryService;
import projects.smart_grocery.pantry.Product;
import projects.smart_grocery.pantry.ProductService;
import projects.smart_grocery.pantry.UnitType;
import projects.smart_grocery.pantry.dto.CreatePantryItemRequest;
import projects.smart_grocery.user.User;
import projects.smart_grocery.user.UserService;
import projects.smart_grocery.web.dto.PantryItemForm;
import projects.smart_grocery.web.dto.RegisterForm;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class WebAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final ProductService productService;
    private final PantryService pantryService;
    private final HouseholdService householdService;
    private final HouseholdMemberService householdMemberService;
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
                             Model model,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        Optional<DashboardContext> context = resolveDashboardContext(principal, request, response);
        if (context.isEmpty()) return "redirect:/login?sessionReset";
        Household household = context.get().household();

        List<PantryItem> items = pantryService.getByHousehold(household.getId());
        List<Product> products = productService.findAllProducts();

        if (!model.containsAttribute("pantryItemForm")) {
            PantryItemForm form = new PantryItemForm();
            form.setUnit(UnitType.PCS);
            form.setMinQtyThreshold(java.math.BigDecimal.ONE);
            model.addAttribute("pantryItemForm", form);
        }

        model.addAttribute("email", context.get().user().getEmail());
        model.addAttribute("householdName", household.getName());
        model.addAttribute("pantryItems", items);
        model.addAttribute("products", products);
        model.addAttribute("unitTypes", UnitType.values());
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
            model.addAttribute("email", context.get().user().getEmail());
            model.addAttribute("householdName", household.getName());
            model.addAttribute("pantryItems", pantryService.getByHousehold(household.getId()));
            model.addAttribute("products", productService.findAllProducts());
            model.addAttribute("unitTypes", UnitType.values());
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
            model.addAttribute("email", context.get().user().getEmail());
            model.addAttribute("householdName", household.getName());
            model.addAttribute("pantryItems", pantryService.getByHousehold(household.getId()));
            model.addAttribute("products", productService.findAllProducts());
            model.addAttribute("unitTypes", UnitType.values());
            return "pantry";
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
}
