CAMPUSCONNECT FINAL

Desktop system for student service requests (enrollment, documents, payments, advising), built with Java Swing and MySQL.


CONTENTS

- CampusConnect_Final/                             NetBeans project
- CampusConnect_Final/sql/schema.sql               creates the database and table
- CampusConnect_Final/sql/seed_data.sql            sample data (optional)
- docs/Test Cases & Screenshots - CampusConnect.pdf
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
   Note: schema.sql deletes existing CampusConnect data.
3. In CampusConnect_Final/, copy db.properties.example to db.properties and set db.password
   (leave it empty if root has no password). This file is not committed.


RUN

- Open CampusConnect_Final in NetBeans and press F6.
- Tests: press Alt+F6 (21 JUnit tests, no database needed).


TROUBLESHOOTING

- "Database settings file not found"   -> create db.properties (Setup step 3)
- "Communications link failure"        -> start the MySQL84 service, then click Refresh
- "Access denied for user 'root'"      -> correct db.password
- "Unknown database 'campusconnect'"   -> run sql/schema.sql


MILESTONES

This release was built through four milestones (TSA1 to TSA4).
See docs/Milestone_Evidence.pdf and docs/Traceability_Matrix.pdf.
