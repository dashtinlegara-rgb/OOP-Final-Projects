BOOKBRIDGE FINAL

Desktop library system for books, members, loans and reservations, built with Java Swing and MySQL.


CONTENTS

- BookBridge_Final/                             NetBeans project
- BookBridge_Final/sql/schema.sql               creates the database and tables
- BookBridge_Final/sql/seed_data.sql            sample data (optional)
- docs/Test Cases & Screenshots - BookBridge.pdf
- docs/Traceability_Matrix.pdf
- docs/UML DIAGRAM.pdf
- docs/Milestone_Evidence.pdf


REQUIREMENTS

- JDK 21
- Apache NetBeans (internet needed on the first build to download the MySQL driver and JUnit)
- MySQL Server 8 and MySQL Workbench


SETUP

1. Start MySQL (Windows service MySQL84).
2. In MySQL Workbench, run sql/schema.sql, then sql/seed_data.sql for sample data.
   Note: schema.sql deletes existing BookBridge data.
3. In BookBridge_Final/, copy db.properties.example to db.properties and set db.password
   (leave it empty if root has no password). This file is not committed.


RUN

- Open BookBridge_Final in NetBeans and press F6.
- Tests: press Alt+F6 (20 JUnit tests, no database needed).


TROUBLESHOOTING

- "Cannot read database settings"      -> create db.properties (Setup step 3)
- "Communications link failure"        -> start the MySQL84 service, then click Refresh
- "Access denied for user 'root'"      -> correct db.password
- "Unknown database 'bookbridge'"      -> run sql/schema.sql


MILESTONES

This release was built through four milestones (TSA1 to TSA4).
See docs/Milestone_Evidence.pdf and docs/Traceability_Matrix.pdf.
