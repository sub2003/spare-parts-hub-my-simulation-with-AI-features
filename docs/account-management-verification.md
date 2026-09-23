# Account Management Subset — Verification

Completed static checks in the build workspace:

- 33 Thymeleaf/HTML templates parsed without HTML parser errors.
- No missing local `/css`, `/js`, or `/images` references were detected.
- Modified Java sources have balanced braces.
- `javac` syntax-oriented diagnostics showed no syntax-pattern errors; full symbol resolution cannot run without Spring/Maven dependencies.
- Maven Wrapper compile was attempted, but the environment could not download Maven 3.9.16 from Maven Central, so a real Maven compile/startup is **not claimed**.

Run locally after applying the updated simulation schema:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Then verify:

1. Active staff can log in.
2. Seeded `inactive@sparepartshub.lk` cannot log in.
3. Click the navbar user chip to open `/profile`.
4. Update profile name/email.
5. Change own password using the current password.
6. Admin opens `/reporting/staff`.
7. Admin creates a staff account and assigns a role.
8. Admin changes another user's role.
9. Admin deactivates/reactivates another account.
10. Admin cannot deactivate their own currently signed-in account.
11. The last active Admin cannot be removed from the Admin role or deactivated.
12. Admin can issue a temporary password.
13. Check `/reporting` audit activity for profile/account/security actions.
