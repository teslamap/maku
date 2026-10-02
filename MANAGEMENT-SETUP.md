# Makasia — Firebase Spark setup

## Included
- `management.html`: Firebase Email/Password login and staff read view for products/orders.
- `firestore.rules`: role checks use the protected `users/{uid}.role` profile field; only the primary admin can write staff roles.
- No Cloud Functions are required or deployed, so this setup does not require upgrading to Blaze.

## Add the one manager manually (Spark)
1. In Firebase Console → Authentication → Sign-in method, enable Email/Password.
2. Go to Authentication → Users → Add user. Create the manager using their email and a temporary password. Do not share passwords in chat.
3. Copy the new user's UID.
4. Go to Firestore Database → Data → `users` → Add document. Set the document ID to that exact UID.
5. Add fields such as `name` (string), `email` (string), and `role` (string) with exact value `manager`.
6. Publish the repository's `firestore.rules` in Firebase Console → Firestore Database → Rules. Sign in at `management.html` with that account.

The manager cannot assign or change their own role through the site: the rules permit only the primary admin UID to write role fields. Manual role edits in Firebase Console are performed by the project owner.

## Important limitations
- This Spark-compatible arrangement uses a Firestore profile role instead of custom claims. Do not expose role editing to managers.
- The current Management page displays product/order data but is not yet a full CRUD/order-status interface.
- The storefront still needs Firebase Authentication integration before customer registration and authenticated order creation will work under these rules. Test records can be cleared or migrated as desired; back them up first if you want to retain them.
- Review the exact storefront document schema and rules before using with real customers.
- Firebase Console role changes bypass Firestore rules because they are made by the project owner. Restrict project-owner access and enable account security.


## Customer checkout authentication
The storefront signs guests in anonymously with Firebase Authentication so Firestore rules can validate each order's `userId`. In Firebase Console → Authentication → Sign-in method, enable **Anonymous** alongside Email/Password. This uses Firebase Authentication and Firestore without Cloud Functions or Blaze. Publish the updated Firestore rules only after reviewing them in the Console, then test guest checkout and manager sign-in in a private browser window.
