# Account Management Subset — Changed Files

## New Java files
- `src/main/java/com/sliit/sparepartshub/repository/AuditLogRepository.java`
- `src/main/java/com/sliit/sparepartshub/security/ActiveStaffFilter.java`
- `src/main/java/com/sliit/sparepartshub/web/ProfileController.java`
- `src/main/java/com/sliit/sparepartshub/web/ProfileService.java`
- `src/main/java/com/sliit/sparepartshub/reporting/dto/StaffAccountForm.java`
- `src/main/java/com/sliit/sparepartshub/reporting/controller/StaffAccountController.java`
- `src/main/java/com/sliit/sparepartshub/reporting/service/StaffAccountService.java`

## Modified Java files
- `src/main/java/com/sliit/sparepartshub/entity/User.java`
- `src/main/java/com/sliit/sparepartshub/repository/UserRepository.java`
- `src/main/java/com/sliit/sparepartshub/security/CustomUserDetailsService.java`
- `src/main/java/com/sliit/sparepartshub/security/CustomUserPrincipal.java`
- `src/main/java/com/sliit/sparepartshub/security/SecurityConfig.java`

## New templates
- `src/main/resources/templates/profile/index.html`
- `src/main/resources/templates/profile/password.html`
- `src/main/resources/templates/reporting/staff-list.html`
- `src/main/resources/templates/reporting/staff-form.html`

## Modified UI files
- `src/main/resources/templates/fragments/layout.html`
- `src/main/resources/templates/reporting/index.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/static/css/app-shell.css`

## Database
- `database/schema-simulation.sql`
- `database/simulation-seed.sql`
- `database/patch-account-management.sql` (new, optional one-time patch for an existing DB)

## Docs/config
- `src/main/resources/application.properties.example` (safe placeholder only)
- `docs/account-management-subset.md`
- `docs/account-management-changed-files.md`
