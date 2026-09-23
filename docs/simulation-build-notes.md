# Simulation Build / Verification Notes

## What was verified in this build environment

- All Java source files were compiled with `javac` against local minimal Spring/JPA API stubs: **0 Java compile errors**. This validates Java syntax, project-internal class references, method names, and constructor wiring used by the source tree.
- All 29 Thymeleaf/HTML templates were parsed structurally: **0 parse failures**.
- Referenced local `/css/**`, `/js/**`, and `/images/**` assets were checked: **no missing local asset files**.
- The seeded BCrypt value was verified against the documented local demo password.
- The staff and supplier security configurations are separate, with supplier portal `@Order(1)` and staff `@Order(2)`.

## What could not be verified here

A real Maven build could not run in this environment because the Maven Wrapper attempted to download Maven 3.9.16 from Maven Central and outbound DNS/network access is unavailable here. Therefore a real Spring Boot startup against MySQL was **not** claimed.

On the Windows development machine, run:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

Then verify there are no Hibernate `ddl-auto=validate` errors against `spareparts_mysimulation`.

## Deliberate demo limitations

- QR processing uses the human-readable pick-ticket code as a manual scan fallback; no camera scanner was added.
- Supplier catalog submission stores a simulation catalog reference/path instead of implementing production file storage.
- Reporting anomaly detection is deterministic rule-based logic, not AI/ML.
- This is a local integration simulation and should not be presented as the teammates' final merged production branches.
