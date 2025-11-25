-- Insert second admin
INSERT INTO admin (email, password, name) VALUES ('admin2@library.com', 'admin123', 'Admin 2');

-- Get current state of all admins
SELECT id, email, name FROM admin;

-- Get count of books per admin
SELECT admin_id, COUNT(*) as book_count FROM books GROUP BY admin_id;

-- Get books for admin 1 (first 5)
SELECT isbn, title, admin_id FROM books WHERE admin_id = 1 LIMIT 5;

-- Move some books from admin1 to admin2 (if you want to test)
-- UPDATE books SET admin_id = 2 WHERE admin_id = 1 AND isbn IN ('ISBN-001', 'ISBN-002', 'ISBN-003');
