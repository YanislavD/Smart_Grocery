(function () {
    const ENTER_KEY = "auth-enter-target";
    const path = window.location.pathname;
    const isLoginPage = path === "/login";
    const isRegisterPage = path === "/register";

    if (!isLoginPage && !isRegisterPage) return;

    const targetOnEnter = sessionStorage.getItem(ENTER_KEY);
    const body = document.body;

    if ((isLoginPage && targetOnEnter === "login") || (isRegisterPage && targetOnEnter === "register")) {
        body.classList.add("auth-entering");
        sessionStorage.removeItem(ENTER_KEY);
        window.setTimeout(() => body.classList.remove("auth-entering"), 420);
    }

    document.addEventListener("click", function (event) {
        const anchor = event.target.closest("a[href]");
        if (!anchor) return;
        if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
        if (anchor.target && anchor.target !== "_self") return;

        const href = anchor.getAttribute("href");
        if (!href) return;

        const targetPath = href.startsWith("http") ? new URL(href).pathname : href;
        if (targetPath !== "/login" && targetPath !== "/register") return;
        if (targetPath === path) return;

        event.preventDefault();
        sessionStorage.setItem(ENTER_KEY, targetPath === "/login" ? "login" : "register");
        body.classList.add("auth-leaving");
        window.setTimeout(() => {
            window.location.assign(targetPath);
        }, 290);
    });
})();
