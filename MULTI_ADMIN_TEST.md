# Multi-Admin Testing Guide

## Test Scenario: Admin-Only Data Filtering

### Test Setup
✅ **Completed**: Multi-admin test data created:
- **Admin 1** (admin@library.com / admin123): 97 books + 100+ copies
- **Admin 2** (admin2@library.com / admin123): 3 books (Clean Code, Design Patterns, Effective Java) + 6 copies
- **Admin 3** (karim@example.com / *): 0 books (not used in this test)

### Test Steps

#### Step 1: Login as Admin 1 and Verify Data Isolation
1. Start the app (it's running in background: `.\mvnw.cmd javafx:run`)
2. Click **Login as Admin** button
3. Enter: `admin@library.com` / `admin123`
4. After login, click **Books** in the sidebar
   - ✅ **Expected**: Should see ~97 books (all of Admin 1's books)
5. Click **Copies** in the sidebar
   - ✅ **Expected**: Should see ~100+ copies (all of Admin 1's copies)
6. Click **Members** in the sidebar
   - ✅ **Expected**: Should see 10 members (all linked to Admin 1)
7. Click **Borrowings** in the sidebar
   - ✅ **Expected**: Should see borrowing records for Admin 1's books/members

#### Step 2: Logout and Login as Admin 2
1. Click the **Logout** button (or menu)
2. Click **Login as Admin** button again
3. Enter: `admin2@library.com` / `admin123`
4. After login, click **Books** in the sidebar
   - ✅ **CRITICAL TEST**: Should see **ONLY 3 books** (Clean Code, Design Patterns, Effective Java)
   - ❌ **BUG**: If you see 97+ books, admin_id filtering is not working
5. Click **Copies** in the sidebar
   - ✅ **CRITICAL TEST**: Should see **ONLY 6 copies** (for Admin 2's 3 books)
   - ❌ **BUG**: If you see 100+ copies, admin_id filtering is not working
6. Click **Members** in the sidebar
   - ✅ **CRITICAL TEST**: Should see **NO members** (0 members linked to Admin 2)
   - ❌ **BUG**: If you see 10 members, admin_id filtering is not working
7. Click **Borrowings** in the sidebar
   - ✅ **CRITICAL TEST**: Should see **NO borrowing records** (none for Admin 2)
   - ❌ **BUG**: If you see borrowing records, admin_id filtering is not working

#### Step 3: Create Books Under Admin 2
1. Click **Add Book** button
2. Fill in book details (e.g., ISBN: 999-001, Title: "Test Book")
3. Click **Add**
4. Back to Books list
   - ✅ **CRITICAL TEST**: New book should appear (now Admin 2 has 4 books)
   - Admin 1 still has 97 books (verified in Step 1)

### Success Criteria
✅ **PASS** if:
1. Admin 1 sees 97 books, Admin 2 sees 3 books (not shared)
2. Admin 1 sees 10 members, Admin 2 sees 0 members (not shared)
3. Admin 1 sees borrowings, Admin 2 sees none (not shared)
4. New books created by Admin 2 only appear to Admin 2

❌ **FAIL** if:
1. Admin 1 and Admin 2 see the same data (filtering not working)
2. Data from one admin appears under another admin's account

### Code Locations (Admin ID Filtering)
The following controllers implement admin_id filtering:
- `/src/main/java/com/libraryms/controller/MembersController.java` (line ~58)
- `/src/main/java/com/libraryms/controller/BooksController.java` (line ~58)
- `/src/main/java/com/libraryms/controller/BorrowingsController.java` (line ~70)
- `/src/main/java/com/libraryms/controller/CopiesController.java` (line ~58)

All use pattern:
```java
Integer adminId = Session.getAdminId();
if (adminId == null) {
    return; // Not logged in as admin
}
// Use PreparedStatement with: WHERE [table].admin_id = ?
pst.setInt(paramIndex, adminId);
```

### Cleanup (Optional)
To reset test data and start fresh:
1. Delete `libraryms.db` file
2. Run app again (will auto-seed with Admin 1's data)
3. Run `TestMultiAdmin` to recreate test scenario

