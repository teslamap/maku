# Makasia Admin V2 — Product & Technical Specification

Status: foundation / implementation in progress  
Branch: `rebuild/admin-v2-foundation`

## Product goal
Replace the legacy desktop and mobile admin experiences with one maintainable admin application that adapts to desktop, mobile browsers, and the existing Android WebView APK. Keep the customer storefront, manager experience, Firebase project, and existing data intact during migration.

## Core principles
- One responsive web client and one shared domain model; avoid separate desktop/mobile implementations.
- Firebase Authentication for identity. Never use a shared hard-coded password or client-only role checks as security boundaries.
- Enforce every privileged operation in Firestore Security Rules and, where required, trusted server-side functions. Hiding a button is UX, not authorization.
- Least privilege, explicit permissions, auditability, validation, recoverability, and accessible UI.
- Migrate in parallel; retire legacy admin entry points only after parity, security validation, and APK cutover.

## Modules & functions
| Module | Scope |
|---|---|
| Overview | Sales/revenue, order counts, average order, recent orders, low-stock alerts, activity and system notices |
| Products | Create/read/update/archive, Georgian/English name and description readiness, category, price/old price, images, color/size, SKU, status |
| Inventory | Stock adjustments, reason, threshold, movement history, low-stock view |
| Orders | Search/filter/date range, details, customer/items/delivery, status transitions, cancellation and refund tracking when supported |
| Customers | Customer profile, contact, order history, account state, internal support notes with restricted access |
| Reviews | Queue, approve/hide, remove where policy permits, moderation reason |
| Promotions | Create/edit/disable/delete, code, percent/fixed discount, validity, minimum basket, usage cap, per-customer cap, active state |
| Finance | Date-filtered revenue and order totals, cancelled-order exclusion, refunds/returns only when integrated, export |
| Catalog content | Categories, banners, featured products, campaign copy |
| Staff & access | Invite/disable staff, role assignment, permission review, session revocation where supported |
| Audit log | Actor, action, entity, timestamp, before/after summary, reason; append-only to clients |
| Reports | Orders, sales, stock and promotion performance; CSV export |
| Notifications | New order, low stock, failed operations, access/security events |
| Settings | Store profile, delivery, currency, integrations and security options |

## Roles & default permissions
Roles are starting presets; permissions should be independently assignable by an Owner. Deny by default.

| Capability | Owner | Admin | Manager | Inventory | Order Ops | Marketing | Finance | Support | Auditor |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Dashboard | Full | Yes | Yes | Stock view | Order view | Campaign view | Finance view | Support view | Read |
| Products | Full | Yes | Yes | Read | Read | Read | — | Read | Read |
| Inventory | Full | Yes | Yes | Manage | — | — | Read | — | Read |
| Orders | Full | Yes | Manage | Read | Manage | Read | Read | Support | Read |
| Customers | Full | Yes | Limited | — | Limited | — | Limited | Support | Read |
| Reviews | Full | Yes | Moderate | — | — | Moderate | — | Flag | Read |
| Promo codes | Full | Yes | **No** | No | No | No by default | No | No | Read |
| Finance | Full | Yes | Summary | — | — | Campaign metrics | Manage | — | Read |
| Catalog content | Full | Yes | Manage | — | — | Manage | — | — | Read |
| Staff/roles | Full | Limited* | No | No | No | No | No | No | No |
| Audit logs | Full | Read | Scoped read | Own actions | Own actions | Own actions | Finance scope | Own actions | Read |
| Settings | Full | Limited | No | No | No | No | No | No | No |

*Admin staff-management scope must be explicitly granted by Owner; Owner-only recovery and ownership transfer. Manager never receives promo-code permissions by default or through broad manager access.

## Data model (target; migration mapping required)
Existing collections confirmed: `products`, `orders`, `users`, `reviews`; `promos` is used by legacy admin. Proposed additions: `staff`, `roles`, `inventory_movements`, `audit_logs`, `notifications`, `settings`. Do not rename or bulk-transform existing collections until actual field usage, queries, indexes, and customer/manager compatibility are audited.

## Security requirements
- Firebase Auth; staff membership stored separately from customer-editable profile fields.
- Server-enforced role/permission checks, least privilege, deny-by-default rules.
- Customer profile updates cannot change staff role, admin flag, or permissions.
- Promo writes restricted to explicitly authorized admin principals; manager denied.
- Validate allowed fields, types, status transitions, prices, stock changes, and ownership.
- Audit sensitive writes; do not permit clients to edit/delete audit records.
- MFA for privileged staff where available; re-authentication for sensitive actions.
- Rate limiting / abuse controls through trusted backend for sensitive workflows.
- Confirmation for destructive actions, soft-delete/archive where suitable, backup and restore plan.
- No secrets in client bundle; monitor errors and access-denied events.

## UX requirements
- Responsive desktop sidebar and mobile navigation, touch-friendly controls, loading/empty/error states, Georgian-first labels, accessible contrast and keyboard support.
- Consistent design system and validation messages.
- Search/filter/sort/pagination for large collections; avoid loading all documents at once.
- Clear permission-denied and offline states; no false success toast.

## Delivery plan
1. Foundation: this specification, responsive shell, authentication gate, route/module framework.
2. Authorization: staff model, explicit permission map, Firestore rules review and emulator tests.
3. Domain modules: dashboard, products, inventory, orders, customers, reviews, promotions, finance, content, staff, audit, reports, notifications, settings.
4. Data compatibility: map real legacy fields and verify customer + manager flows.
5. Mobile/APK: point admin flavor to the new responsive client; verify WebView behavior and offline/error states.
6. QA: role matrix tests, rules emulator tests, CRUD/status validation, mobile viewport/WebView, regression and backup/restore.
7. Cutover: switch admin entry points; remove `admin.html` and `mobile-admin.html` only after verified replacement. Preserve `management-mobile.html`, storefront, Firebase data, and rollback path.

## Current implementation boundary
The foundation is separate from the legacy admin. No existing admin page, Firestore data, manager page, or installed APK is deleted or modified by this initial step.
