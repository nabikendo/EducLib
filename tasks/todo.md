# EducLib — iPad-inspired tablet UI

- [x] Design tablet landscape mockups in Figma.
- [x] Add editorial pastel palette and motion tokens.
- [x] Adapt library welcome header for wide tablets without breaking phone layout.
- [ ] Replace placeholder symbols with consistent vector icons.
- [x] Implement responsive dark sidebar for tablets (>= 600 dp width).
- [x] Implement animated master/detail split view for wide landscape tablets; full history remains available in existing dialog.
- [ ] Add true shared-element transitions between book covers and details (panel slide/fade implemented).
- [x] Add CI triggers for feature branch and pull requests.
- [ ] Run Android Gradle compilation, Compose UI tests and tablet screenshots (no successful run observed yet).
- [ ] Review dark mode, accessibility, reduced motion and keyboard navigation.

Implementation is isolated on branch `design/ipados-tablet-editorial`; not yet merged.

## Audit findings

- [x] Loan creation, return and deletion previously used multiple DB writes without an enclosing transaction; wrapped in Room transactions.
- [x] Loan creation previously accepted quantity <= 0; rejected.
- [ ] Check restore of backup images: portable backup explicitly drops book and borrower photo paths.
- [ ] Check dialog overflow and screen-reader navigation on landscape tablets.
- [ ] Confirm Android 36.1 SDK availability in CI environment.

## Update distribution

- [x] In-app APK download from stable GitHub Release, Android installer handoff and next-day retry.
- [x] Validate APK package name before opening installer.
- [ ] Verify installer flow on physical tablet, APK signing continuity and stable release secrets.
