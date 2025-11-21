package com.libraryms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class InitDB {
    private static final String DB_PATH = "libraryms.db";

    public static void initializeDatabase() {
        String url = "jdbc:sqlite:" + DB_PATH;
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);

            boolean needSeed = true;
            // If the DB already has an admin row, assume it's seeded and skip reseed
            try (Statement checkStmt = conn.createStatement()) {
                try (ResultSet rs = checkStmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='admin'")) {
                    if (rs.next()) {
                        try (ResultSet rs2 = checkStmt.executeQuery("SELECT COUNT(*) FROM admin")) {
                            if (rs2.next() && rs2.getInt(1) > 0) {
                                needSeed = false;
                            }
                        } catch (Exception ignored) {
                            // table exists but count failed => fall back to seeding
                        }
                    }
                } catch (Exception ignored) {
                    // sqlite_master query failed => will attempt to seed
                }
            }

            if (needSeed) {
                try (Statement stmt = conn.createStatement()) {
                    // Only create/drop tables when seeding is required
                    dropCreateTables(stmt);
                }
                seed(conn);
                conn.commit();
                System.out.println("Database seeded successfully.");
            } else {
                System.out.println("Database already initialized - skipping seed.");
                // Ensure new columns exist on upgrades (e.g., add 'cin' to users, 'password' to users)
                try {
                    ensureUsersCinColumn(conn);
                    ensureUsersPasswordColumn(conn);
                    ensureBorrowingBookTitleColumn(conn);
                } catch (Exception e) {
                    // log and continue
                    System.err.println("Failed to ensure schema updates: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureUsersCinColumn(Connection conn) {
        try (var stmt = conn.createStatement()) {
            try (var rs = stmt.executeQuery("PRAGMA table_info('users')")) {
                boolean hasCin = false;
                while (rs.next()) {
                    String name = rs.getString("name");
                    if ("cin".equalsIgnoreCase(name)) { hasCin = true; break; }
                }
                if (!hasCin) {
                    stmt.execute("ALTER TABLE users ADD COLUMN cin TEXT");
                    System.out.println("Added 'cin' column to users table.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring cin column: " + e.getMessage());
        }
    }

    private static void ensureUsersPasswordColumn(Connection conn) {
        try (var stmt = conn.createStatement()) {
            try (var rs = stmt.executeQuery("PRAGMA table_info('users')")) {
                boolean hasPassword = false;
                while (rs.next()) {
                    String name = rs.getString("name");
                    if ("password".equalsIgnoreCase(name)) { hasPassword = true; break; }
                }
                if (!hasPassword) {
                    stmt.execute("ALTER TABLE users ADD COLUMN password TEXT");
                    System.out.println("Added 'password' column to users table.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring password column: " + e.getMessage());
        }
    }

    private static void dropCreateTables(Statement stmt) throws Exception {
        // Drop existing tables (if any)
        String[] dropStatements = {
            "DROP TABLE IF EXISTS borrowing",
            "DROP TABLE IF EXISTS copies",
            "DROP TABLE IF EXISTS books",
            "DROP TABLE IF EXISTS users",
            "DROP TABLE IF EXISTS admin"
        };
        for (String sql : dropStatements) {
            try {
                stmt.execute(sql);
            } catch (Exception ignored) {
            }
        }

        // Create admin table
        stmt.execute("CREATE TABLE admin (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "name TEXT NOT NULL)");

        // Create users table
        stmt.execute("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "phone TEXT UNIQUE NOT NULL, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE, " +
                "birthdate DATE, " +
                "type TEXT CHECK (type IN ('Standard', 'Premium')) DEFAULT 'Standard', " +
                "borrow_limit INTEGER DEFAULT 3, " +
                "password TEXT NOT NULL, " +
                "cin TEXT)");

        // Create books table
        stmt.execute("CREATE TABLE books (" +
                "isbn TEXT PRIMARY KEY, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "total_copies INTEGER DEFAULT 1, " +
                "available_copies INTEGER DEFAULT 1)");

        // Create copies table (no location column)
        stmt.execute("CREATE TABLE copies (" +
            "copy_id TEXT PRIMARY KEY, " +
            "isbn TEXT NOT NULL, " +
            "status TEXT CHECK (status IN ('Available', 'Borrowed', 'Lost')) DEFAULT 'Available', " +
            "FOREIGN KEY(isbn) REFERENCES books(isbn) ON DELETE CASCADE)");

        // Create borrowing table
        stmt.execute("CREATE TABLE borrowing (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "copy_id TEXT NOT NULL, " +
                "user_phone TEXT NOT NULL, " +
                "admin_id INTEGER, " +
                "borrow_date DATE NOT NULL DEFAULT CURRENT_DATE, " +
                "due_date DATE NOT NULL, " +
                "return_date DATE, " +
                "status TEXT CHECK (status IN ('In Progress', 'Late', 'Returned')) DEFAULT 'In Progress', " +
                "FOREIGN KEY(copy_id) REFERENCES copies(copy_id) ON DELETE CASCADE, " +
                "FOREIGN KEY(user_phone) REFERENCES users(phone) ON DELETE CASCADE, " +
                "FOREIGN KEY(admin_id) REFERENCES admin(id) ON DELETE SET NULL)");
    }

    private static void ensureBorrowingBookTitleColumn(Connection conn) {
        try (var stmt = conn.createStatement()) {
            try (var rs = stmt.executeQuery("PRAGMA table_info('borrowing')")) {
                boolean hasBookTitle = false;
                while (rs.next()) {
                    String name = rs.getString("name");
                    if ("book_title".equalsIgnoreCase(name)) { hasBookTitle = true; break; }
                }
                if (!hasBookTitle) {
                    stmt.execute("ALTER TABLE borrowing ADD COLUMN book_title TEXT");
                    System.out.println("Added 'book_title' column to borrowing table.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring borrowing.book_title column: " + e.getMessage());
        }
    }

    private static void seed(Connection conn) throws Exception {
        // Admin
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO admin (email, password, name) VALUES ('admin@library.com', 'admin123', 'Super Admin')");
        }

        // 10 Members with real data
        List<String> memberPhones = new ArrayList<>();
        String[] realNames = {
            "Ahmed Ben Ali", "Fatima Saida", "Mohamed Hassan", "Leila Amira", "Karim Youssef",
            "Noor Hamza", "Ibrahim Tarek", "Soumaya Abdel", "Zaineb Marwa", "Hassan Ahmed"
        };
        String[] realCins = {
            "12345678", "23456789", "34567890", "45678901", "56789012",
            "67890123", "78901234", "89012345", "90123456", "01234567"
        };
        String[] realEmails = {
            "ahmed.benali@email.com", "fatima.saida@email.com", "m.hassan@email.com", "leila.amira@email.com", "karim.youssef@email.com",
            "noor.hamza@email.com", "ibrahim.tarek@email.com", "soumaya.abdel@email.com", "zaineb.marwa@email.com", "hassan.ahmed@email.com"
        };

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (phone, name, email, birthdate, type, borrow_limit, password, cin) VALUES (?,?,?,?,?,?,?,?)")) {
            for (int i = 0; i < 10; i++) {
                String phone = String.format("+216%08d", 20000000 + i);
                memberPhones.add(phone);
                String password = "password123";
                String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
                ps.setString(1, phone);
                ps.setString(2, realNames[i]);
                ps.setString(3, realEmails[i]);
                ps.setString(4, LocalDate.of(1985 + (i % 20), (i % 12) + 1, (i % 28) + 1).toString());
                ps.setString(5, (i % 2 == 0) ? "Standard" : "Premium");
                ps.setInt(6, (i % 2 == 0) ? 3 : 5);
                ps.setString(7, hashedPassword);
                ps.setString(8, realCins[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // 100 Books with real data
        String[] realTitles = {
            "Clean Code", "Design Patterns", "Effective Java", "The Pragmatic Programmer", "Code Complete",
            "Refactoring", "The C Programming Language", "Introduction to Algorithms", "The Art of Computer Programming", "Structure and Interpretation",
            "1984", "To Kill a Mockingbird", "The Great Gatsby", "Pride and Prejudice", "Wuthering Heights",
            "The Catcher in the Rye", "Animal Farm", "Lord of the Flies", "The Hobbit", "Harry Potter and the Sorcerer's Stone",
            "Business Adventures", "Good to Great", "The Lean Startup", "Zero to One", "Thinking, Fast and Slow",
            "Sapiens", "Educated", "Atomic Habits", "The Power of Now", "Man's Search for Meaning",
            "Cosmos", "A Brief History of Time", "The Grand Design", "The Selfish Gene", "Gödel, Escher, Bach",
            "Thinking in Systems", "The Structure of Scientific Revolutions", "The Art of Science", "Why Science Matters", "Science Rules",
            "The History of Rome", "The Fall of the Roman Empire", "Napoleon", "The History of England", "The Black Death",
            "Outlander", "All the Light We Cannot See", "The Nightingale", "The Book Thief", "The Midnight Library",
            "A Man Called Ove", "The Seven Husbands of Evelyn Hugo", "Lessons in Chemistry", "Remarkably Bright", "The Song of Achilles",
            "Braiding Sweetgrass", "A Billion Wicked Thoughts", "Quiet", "The Gifts of Imperfection", "Becoming",
            "The Road Less Traveled", "The Power of Habit", "Mindset", "You Are a Badass", "Emotional Intelligence",
            "Dune", "Foundation", "Neuromancer", "The Martian", "Ender's Game",
            "The Three-Body Problem", "Hyperion", "Leviathan Wakes", "Snow Crash", "Cryptonomicon",
            "The Expanse Series", "Old Man's War", "Blindsight", "Revelation Space", "The Diamond Age",
            "A Memory Called Empire", "The Goblin Emperor", "Piranesi", "American Gods", "The Name of the Wind",
            "Mistborn", "The Way of Kings", "Stormlight Archive", "Gideon the Ninth", "Project Hail Mary",
            "Shogun", "The Count of Monte Cristo", "Les Misérables", "War and Peace", "Anna Karenina",
            "The Odyssey", "The Iliad", "Divine Comedy", "Paradise Lost", "The Canterbury Tales"
        };

        String[] realAuthors = {
            "Robert C. Martin", "Gang of Four", "Joshua Bloch", "David Thomas", "Steve McConnell",
            "Martin Fowler", "Brian Kernighan", "Thomas Cormen", "Donald Knuth", "Harold Abelson",
            "George Orwell", "Harper Lee", "F. Scott Fitzgerald", "Jane Austen", "Emily Brontë",
            "J.D. Salinger", "George Orwell", "William Golding", "J.R.R. Tolkien", "J.K. Rowling",
            "John Brooks", "Jim Collins", "Eric Ries", "Peter Thiel", "Daniel Kahneman",
            "Yuval Noah Harari", "Tara Westover", "James Clear", "Eckhart Tolle", "Viktor Frankl",
            "Carl Sagan", "Stephen Hawking", "Stephen Hawking", "Richard Dawkins", "Douglas Hofstadter",
            "Donella Meadows", "Thomas Kuhn", "Lewis Wolpert", "Roger Penrose", "Bill Nye",
            "Michael Parenti", "Peter Heather", "Andrew Roberts", "Simon Schama", "Johannes Benedictow",
            "Diana Gabaldon", "Anthony Doerr", "Kristin Hannah", "Markus Zusak", "Matt Haig",
            "Fredrik Backman", "Taylor Jenkins Reid", "Bonnie Garmus", "Rebecca Westcott", "Ariadne",
            "Robin Wall Kimmerer", "Orit Gadiesh", "Susan Cain", "Brené Brown", "Michelle Obama",
            "M. Scott Peck", "Charles Duhigg", "Carol Dweck", "Jen Sincero", "Daniel Goleman",
            "Frank Herbert", "Isaac Asimov", "William Gibson", "Andy Weir", "Orson Scott Card",
            "Liu Cixin", "Dan Simmons", "James S.A. Corey", "Neal Stephenson", "Greg Egan",
            "James S.A. Corey", "John Scalzi", "Peter Watts", "Alastair Reynolds", "Neal Stephenson",
            "Arkady Martine", "Katherine Addison", "Susanna Clarke", "Neil Gaiman", "Patrick Rothfuss",
            "Brandon Sanderson", "Brandon Sanderson", "Brandon Sanderson", "Tamsyn Muir", "Andy Weir",
            "James Clavell", "Alexandre Dumas", "Victor Hugo", "Leo Tolstoy", "Leo Tolstoy",
            "Homer", "Homer", "Dante Alighieri", "John Milton", "Geoffrey Chaucer"
        };

        String[] categories = new String[]{
            "Programming", "Fiction", "Business", "Science", "History", "Psychology", "Self-Help"
        };
        List<String> isbns = new ArrayList<>();
        List<Integer> copiesPerBook = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES (?,?,?,?,?,?)")) {
            for (int i = 0; i < 100; i++) {
                String isbn = String.format("978-%04d-%05d", 1000 + (i / 100), 10000 + i);
                String title = realTitles[i % realTitles.length];
                String author = realAuthors[i % realAuthors.length];
                String category = categories[i % categories.length];
                int total = 1 + (i % 10); // 1 to 10 copies per book
                ps.setString(1, isbn);
                ps.setString(2, title + (i >= realTitles.length ? " (ed. " + (i / realTitles.length + 1) + ")" : ""));
                ps.setString(3, author);
                ps.setString(4, category);
                ps.setInt(5, total);
                ps.setInt(6, total); // start with all available, will adjust after borrowings
                ps.addBatch();
                isbns.add(isbn);
                copiesPerBook.add(total);
            }
            ps.executeBatch();
        }

        // Copies for each book
        List<String> availableCopies = new ArrayList<>();
        Map<String, String> copyToTitle = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO copies (copy_id, isbn, status) VALUES (?,?,?)")) {
            for (int i = 0; i < isbns.size(); i++) {
                String isbn = isbns.get(i);
                String title = (i < realTitles.length) ? realTitles[i % realTitles.length] : "";
                int total = copiesPerBook.get(i);
                for (int j = 1; j <= total; j++) {
                    String copyId = String.format("COPY-%05d-%02d", i + 1, j);
                    ps.setString(1, copyId);
                    ps.setString(2, isbn);
                    ps.setString(3, "Available");
                    ps.addBatch();
                    availableCopies.add(copyId);
                    copyToTitle.put(copyId, title);
                }
            }
            ps.executeBatch();
        }

        // Borrowings from Jan 2025 to today
        LocalDate start = LocalDate.of(2025, 1, 5);
        LocalDate today = LocalDate.now();
        int adminId = 1; // first admin

        try (PreparedStatement insertBorrow = conn.prepareStatement(
                 "INSERT INTO borrowing (copy_id, user_phone, admin_id, borrow_date, due_date, return_date, status, book_title) " +
                     "VALUES (?,?,?,?,?,?,?,?)");
             PreparedStatement markBorrowed = conn.prepareStatement(
                     "UPDATE copies SET status='Borrowed' WHERE copy_id=?");
             PreparedStatement markAvailable = conn.prepareStatement(
                     "UPDATE copies SET status='Available' WHERE copy_id=?")) {

            int borrowingsCreated = 0;
            int phoneIdx = 0;

            for (LocalDate d = start; !d.isAfter(today) && !availableCopies.isEmpty(); d = d.plusDays(5)) {
                // Create up to 2 borrowings every 5 days
                for (int k = 0; k < 2 && !availableCopies.isEmpty(); k++) {
                    String copyId = availableCopies.remove(0); // take first available
                    String phone = memberPhones.get(phoneIdx % memberPhones.size());
                    phoneIdx++;

                    LocalDate borrowDate = d;
                    LocalDate dueDate = borrowDate.plusDays(14);

                    LocalDate returnDate = null;
                    String status;

                    boolean willBeReturned = dueDate.isBefore(today.minusDays(10));
                    if (willBeReturned) {
                        // Returned before due date
                        returnDate = borrowDate.plusDays(7 + ((borrowingsCreated + k) % 6));
                        if (!returnDate.isBefore(dueDate)) {
                            returnDate = dueDate.minusDays(1);
                        }
                        status = "Returned";
                    } else if (dueDate.isBefore(today)) {
                        // Overdue and not yet returned
                        status = "Late";
                    } else {
                        // Still in progress
                        status = "In Progress";
                    }

                    insertBorrow.setString(1, copyId);
                    insertBorrow.setString(2, phone);
                    insertBorrow.setInt(3, adminId);
                    insertBorrow.setString(4, borrowDate.toString());
                    insertBorrow.setString(5, dueDate.toString());
                    if (returnDate == null) {
                        insertBorrow.setNull(6, java.sql.Types.VARCHAR);
                    } else {
                        insertBorrow.setString(6, returnDate.toString());
                    }
                    insertBorrow.setString(7, status);
                    // include book title for easier reporting later
                    String titleForCopy = copyToTitle.getOrDefault(copyId, null);
                    insertBorrow.setString(8, titleForCopy);
                    insertBorrow.addBatch();

                    if ("Returned".equals(status)) {
                        // Temporarily mark borrowed then available again to reflect circulation
                        markBorrowed.setString(1, copyId);
                        markBorrowed.addBatch();
                        markAvailable.setString(1, copyId);
                        markAvailable.addBatch();
                        // Returned copies become available for future loops
                        availableCopies.add(copyId);
                    } else {
                        // Occupied copy stays borrowed (do not put back into available pool)
                        markBorrowed.setString(1, copyId);
                        markBorrowed.addBatch();
                    }

                    borrowingsCreated++;
                }
            }

            insertBorrow.executeBatch();
            markBorrowed.executeBatch();
            markAvailable.executeBatch();
        }

        // Recompute available_copies and total_copies based on copies table
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "UPDATE books SET total_copies = (SELECT COUNT(*) FROM copies c WHERE c.isbn = books.isbn)"
            );
            stmt.executeUpdate(
                "UPDATE books SET available_copies = (SELECT COUNT(*) FROM copies c WHERE c.isbn = books.isbn AND c.status='Available')"
            );
        }
    }
}
