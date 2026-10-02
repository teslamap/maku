# Makasia Authentication & Management rollout

## What is included
- `management.html`: Firebase email/password staff sign-in, product/order read view, and primary-admin-only manager creation UI.
- `functions/`: callable function creates manager accounts using Firebase Admin SDK and assigns the manager custom claim server-side.
- `firestore.rules`: denies guest writes; allows public product/review reads, authenticated users to read/write their own profile and create/read their own orders, managers to read products/orders and update order status only, and the primary admin full access.
- `firebase.json`: Firebase CLI deployment configuration.

## Required Firebase setup
1. Confirm the primary administrator account exists in Firebase Authentication and its UID matches the allowlisted UID in `firestore.rules` and `functions/index.js`.
2. In Authentication > Sign-in method, enable Email/Password.
3. Install Node.js 20 and Firebase CLI, then run `firebase login` and `firebase use makasia` in this repository.
4. From the repository root run `cd functions && npm install && cd ..`, then `firebase deploy --only functions:createManager`.
5. Review `firestore.rules` against your deployed schema, then run `firebase deploy --only firestore:rules`.
6. Host `management.html` on the same HTTPS origin as the storefront. Open it and sign in as the primary admin. Create manager accounts from the panel.

## Important migration note
These rules intentionally stop unauthenticated guest checkout writes. The current storefront uses guest IDs and has no Firebase Authentication, so checkout/review creation will fail until the storefront is migrated to Firebase Auth or guest order creation is moved behind a trusted backend. Do not treat the rules as production-ready for the existing guest checkout flow without that migration. Managers can only update the order `status` field; adapt allowed status values/validation to the exact storefront workflow before production.

The primary admin is allowlisted by UID for bootstrap. Manager claims can only be issued by the callable function, which checks that UID on the server. Never put Admin SDK credentials or service-account keys in frontend code.
