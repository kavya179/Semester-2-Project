import java.io.FileWriter;
import java.io.Reader;
import java.sql.*;
import java.sql.Date;
import java.time.Year;
import java.util.*;

class CrimeDataManager {
    ArrayList<String> password = new ArrayList<>();
    HashMap<Integer, String> record = new HashMap<>();
    HashMap<Integer,Integer> cid = new HashMap<>();
    HashMap<Integer,String> crime_record=new HashMap<>();
    Connection con;

    // Store all crime records in memory
    List<HashMap<String, String>> crimeRecords = new ArrayList<>();
    CrimeDataManager(Connection con) {
        this.con = con;
    }
    // Ds part for the user side
    void set_password_Array() throws Exception {
        String sql1 = "select u_pass from user";
        PreparedStatement pst = con.prepareStatement(sql1);
        ResultSet rs = pst.executeQuery();
        password.clear();
        while (rs.next()) {
            String pass = rs.getString("u_pass");
            password.add(pass);
        }
    }

    void set_record_HashMap() throws Exception{
        String sql2="select u_id,u_mail from user";
        PreparedStatement pst2=con.prepareStatement(sql2);
        ResultSet rs2=pst2.executeQuery();
        record .clear();
        while (rs2.next()){
            int id=rs2.getInt("u_id");
            String mail=rs2.getString("u_mail");
            record.put(id,mail);
        }
    }

    boolean chek_email_exits(String mail){
        if (record.containsValue(mail))
            return  true;
        else
            return false;
    }

    void set_record_as_HashMap() throws Exception {
        String sql = "SELECT u_id, u_mail FROM User";
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        record.clear();
        while (rs.next()) {
            record.put(rs.getInt("u_id"), rs.getString("u_mail"));
        }
    }
    void set_cid()throws Exception{
        String sql="select crime_id,u_id from crime";
        Statement st=con.createStatement();
        ResultSet rs=st.executeQuery(sql);
        while (rs.next()){
            cid.put(rs.getInt("crime_id"),rs.getInt("u_id"));
        }
    }
    void set_crime_record() throws Exception{
        String sql="select crime_id,crime_type from crime";
        PreparedStatement pst=con.prepareStatement(sql);
        ResultSet rs= pst.executeQuery();
        crime_record.clear();
        while (rs.next()){
            crime_record.put(rs.getInt("crime_id"),rs.getString("crime_type"));
        }
    }
    boolean check_cid(int c_cid,int uid){
        if (cid.containsKey(c_cid) && cid.get(c_cid) == uid)
            return true;
        else
            return  false;
    }

    boolean chek_pass(String c_pass){
        if (password.contains(c_pass))
            return false;
        else
            return true;
    }
    // Ds part for admin side
    public void AllCrimeRecords() throws Exception {
        String sql = "SELECT crime_id, u_id, crime_type, description, location, date_reported, status FROM Crime";
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        crimeRecords.clear();
        while (rs.next()) {
            HashMap<String, String> crime = new HashMap<>();
            crime.put("crime_id", String.valueOf(rs.getInt("crime_id")));
            crime.put("u_id", String.valueOf(rs.getInt("u_id")));
            crime.put("crime_type", rs.getString("crime_type"));
            crime.put("description", rs.getString("description"));
            crime.put("location", rs.getString("location"));
            crime.put("date_reported", rs.getDate("date_reported").toString());
            crime.put("status", rs.getString("status") == null ? "Unknown" : rs.getString("status"));
            crimeRecords.add(crime);
        }
    }

    public LinkedHashMap<String, Integer> getTopCrimeLocations() throws Exception {
        String sql = "SELECT location, COUNT(*) AS cnt FROM Crime GROUP BY location ORDER BY cnt DESC LIMIT 5";
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        LinkedHashMap<String, Integer> topLocations = new LinkedHashMap<>();
        while (rs.next()) {
            topLocations.put(rs.getString("location"), rs.getInt("cnt"));
        }
        return topLocations;
    }

    public int getUsersCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM User";
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int getCrimesCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM Crime";
        PreparedStatement pst = con.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }
}
class Admin{
    Scanner sc = new Scanner(System.in);
    CrimeDataManager ds;
    Connection con;

    boolean isAuthenticated = false;
    String adminId;

    Admin(Connection con) {
        this.con = con;
    }
    void run(Connection con) throws Exception {
        this.con = con;
        String sql="CREATE TABLE IF NOT EXISTS Admin (admin_id VARCHAR(50) PRIMARY KEY, admin_name VARCHAR(50),admin_pass VARCHAR(100) NOT NULL)";
        Statement st=con.createStatement();
        st.executeUpdate(sql);
        String sql2="CREATE TABLE IF NOT EXISTS Officer (officer_id INT PRIMARY KEY AUTO_INCREMENT,officer_name VARCHAR(100) NOT NULL,contact VARCHAR(50));";
        Statement st2=con.createStatement();
        st2.executeUpdate(sql2);
        String sql3= "CREATE TABLE IF NOT EXISTS Crime_Officer_Assign (crime_id INT,officer_id INT,assigned_date DATE DEFAULT CURDATE(),PRIMARY KEY (crime_id, officer_id),FOREIGN KEY (crime_id) REFERENCES Crime(crime_id) ON DELETE CASCADE,FOREIGN KEY (officer_id) REFERENCES Officer(officer_id) ON DELETE CASCADE)";
        Statement st3= con.createStatement();
        st3.executeUpdate(sql3);
        ds = new CrimeDataManager(con);
        if (authenticate()) {
            adminPanel();
        } else {
            System.out.println("❌ Authentication failed. Access Denied.");
        }
    }

    boolean authenticate() {
        System.out.println("\n🛡 = Admin Login = 🛡");
        int attempts = 3;
        try {
            while (attempts > 0) {
                System.out.print("Enter Admin ID: ");
                String id = sc.next();
                System.out.print("Enter Admin Password: ");
                String password = sc.next();
                String sql = "SELECT * FROM Admin WHERE admin_id = ? AND admin_pass = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, id);
                pst.setString(2, password);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    System.out.println("✅ Admin authenticated successfully.");
                    isAuthenticated = true;
                    adminId = id;
                    return true;
                } else {
                    attempts--;
                    System.out.println("❌ Invalid credentials. Attempts left: " + attempts);
                }
            }
        } catch (Exception e) {
            System.out.println("Error during authentication: " + e.getMessage());
        }
        return false;
    }

    void adminPanel() throws Exception {
        boolean running = true;
        while (running)
        {
            System.out.println("\n🛡 = Admin Panel = 🛡");
            System.out.println("1️⃣ - Update Crime Status ");
            System.out.println("2️⃣ - Delete a Crime Report ");
            System.out.println("3️⃣ - View All Users ");
            System.out.println("4️⃣ - Delete a User ");
            System.out.println("5️⃣ - View All Crimes From Memory ");
            System.out.println("6️⃣ - Show Real-Time Dashboard ");
            System.out.println("7️⃣ - Assign Officer to Crime ");
            System.out.println("8️⃣ - View The Record Log");
            System.out.println("9️⃣ - Log Out");

            System.out.print("👉 Enter your choice: ");

            String choice = sc.next();
            if (!choice.matches("[1-9]")) {
                System.out.println("⚠ Please enter a number between 1 and 9");
                continue;
            }

            switch (choice) {
                case "1" -> updateCrimeStatus();
                case "2" -> deleteCrime();
                case "3" -> viewAllUsers();
                case "4" -> deleteUser();
                case "5" -> viewCrimesFromMemory();
                case "6" -> RealTimeDashboard();
                case "7" -> assign__Officer();
                case "8" -> view_record_log();
                case "9" -> {
                    running = false;
                    System.out.println("👋 Logging out...");
                }
            }
        }
    }
    void viewCrimesFromMemory() {
        try {

            ds.AllCrimeRecords();

            List<HashMap<String, String>> crimes = ds.crimeRecords;
            if (crimes.isEmpty()) {
                System.out.println("No crime records loaded in memory.");
                return;
            }

            System.out.println("\n--- All Crime Records From Memory ---");

            for (HashMap<String, String> crime : crimes) {
                int crimeId = Integer.parseInt(crime.get("crime_id"));

                // Fetch assigned officers for this crime
                String sql = "SELECT o.officer_name FROM Officer o JOIN Crime_Officer_Assign coa ON o.officer_id = coa.officer_id WHERE coa.crime_id = ?";
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setInt(1, crimeId);
                ResultSet rs = pst.executeQuery();

                List<String> officers = new ArrayList<>();
                while (rs.next()) {
                    officers.add(rs.getString("officer_name"));
                }

                System.out.println("Crime ID: " + crime.get("crime_id"));
                System.out.println("User ID: " + crime.get("u_id"));
                System.out.println("Type: " + crime.get("crime_type"));
                System.out.println("Description: " + crime.get("description"));
                System.out.println("Location: " + crime.get("location"));
                System.out.println("Date Reported: " + crime.get("date_reported"));
                System.out.println("Status: " + crime.get("status"));
                System.out.println("Assigned Officer(s): " + (officers.isEmpty() ? "None" : String.join(", ", officers)));
                System.out.println("---------------------------");
            }
        } catch (Exception e) {
            System.out.println("Error displaying crimes from memory: " + e.getMessage());
        }
    }

    void assign__Officer() {
        try {
            System.out.print("Enter Crime ID to assign: ");
            int crimeId = sc.nextInt();

            // ✅ Check if Crime exists
            String checkCrime = "SELECT COUNT(*) FROM Crime WHERE crime_id = ?";
            PreparedStatement pstcheckCrime = con.prepareStatement(checkCrime);
            pstcheckCrime.setInt(1, crimeId);
            ResultSet rsCheckcrime = pstcheckCrime.executeQuery();
            rsCheckcrime.next();
            int count = rsCheckcrime.getInt(1);
            if (count == 0) {
                System.out.println("❌ Invalid Crime ID. No such Crime exists.");
                return;
            }

            // ✅ Show officers
            String sqlOfficers = "SELECT officer_id, officer_name FROM Officer";
            PreparedStatement pstOfficers = con.prepareStatement(sqlOfficers);
            ResultSet rsOfficers = pstOfficers.executeQuery();
            System.out.println("\n--- Officers List ---");
            while (rsOfficers.next()) {
                System.out.println("ID: " + rsOfficers.getInt("officer_id") + " - Name: " + rsOfficers.getString("officer_name"));
            }

            System.out.print("Enter Officer ID to assign: ");
            int officerId = sc.nextInt();

            // ✅ Check if Officer exists
            String checkOfficer = "SELECT COUNT(*) FROM Officer WHERE officer_id = ?";
            PreparedStatement pstcheckOfficer = con.prepareStatement(checkOfficer);
            pstcheckOfficer.setInt(1, officerId);
            ResultSet rsCheckOfficer = pstcheckOfficer.executeQuery();
            rsCheckOfficer.next();
            int officerCount = rsCheckOfficer.getInt(1);
            if (officerCount == 0) {
                System.out.println("❌ Invalid Officer ID. No such Officer exists.");
                return;
            }

            // ✅ Assign officer
            String sqlAssign = "INSERT INTO Crime_Officer_Assign (crime_id, officer_id) VALUES (?, ?)";
            PreparedStatement pstAssign = con.prepareStatement(sqlAssign);
            pstAssign.setInt(1, crimeId);
            pstAssign.setInt(2, officerId);

            int inserted = pstAssign.executeUpdate();
            if (inserted > 0) {
                System.out.println("✅ Officer assigned to crime successfully.");

                // Update crime status to Assigned
                String sqlUpdateStatus = "UPDATE Crime SET status = 'Assigned' WHERE crime_id = ?";
                PreparedStatement pstUpdate = con.prepareStatement(sqlUpdateStatus);
                pstUpdate.setInt(1, crimeId);
                pstUpdate.executeUpdate();

                System.out.println("ℹ Crime status updated to 'Assigned'.");
            } else {
                System.out.println("⚠ Assignment failed. Please check the Crime ID and Officer ID.");
            }

        } catch (InputMismatchException ime) {
            sc.nextLine(); // clear bad input
            System.out.println("⚠ Invalid option. Please enter numeric values only.");
        } catch (SQLException sqle) {
            System.out.println("⚠ Database error occurred while assigning officer. Please try again.");
        }
    }


    void RealTimeDashboard() {
        try {
            ds.AllCrimeRecords();
            ds.set_record_as_HashMap();

            int currentYear = Year.now().getValue();
            int totalUsers = ds.getUsersCount();
            int totalCrimes = ds.getCrimesCount();


            String sqlAssigned = "SELECT COUNT(DISTINCT crime_id) FROM Crime_Officer_Assign";
            PreparedStatement pstAssigned = con.prepareStatement(sqlAssigned);
            ResultSet rsAssigned = pstAssigned.executeQuery();
            int assignedCrimes = rsAssigned.next() ? rsAssigned.getInt(1) : 0;

            System.out.println("\n🚨=== Real-Time Crime Management Dashboard ===🚨");
            System.out.println("📅 Current Year: " + currentYear);
            System.out.println("👥 Total Registered Users: " + totalUsers);
            System.out.println("⚠ Total Crimes Reported: " + totalCrimes);
            System.out.println("✅ Crimes Assigned to Officers: " + assignedCrimes);
            System.out.println("🗃 Number of Crime Records Loaded Into Memory: " + ds.crimeRecords.size());
            System.out.println("📧 User Emails Registered: " + ds.record.size());

            LinkedHashMap<String, Integer> topLocations = ds.getTopCrimeLocations();
            if (!topLocations.isEmpty()) {
                System.out.println("\nTop Crime Locations:");
                topLocations.forEach((location, count) ->
                        System.out.println(location + ": " + count + " crimes")
                );
            }

            System.out.println("==========================================");
        } catch (Exception e) {
            System.out.println("Error displaying real-time dashboard: " + e.getMessage());
        }
    }




    void updateCrimeStatus() {
        try{ System.out.print("Enter Crime ID to update status: ");
            int crimeId = sc.nextInt();
            sc.nextLine();
            System.out.print("Enter new status (e.g., Pending, In Progress, Resolved): ");
            String status = sc.nextLine();
            String sql = "UPDATE Crime SET status = ? WHERE crime_id = ?";
            try {
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, status);
                pst.setInt(2, crimeId);
                int updated = pst.executeUpdate();
                if (updated > 0) {
                    System.out.println("✅ Crime status updated successfully.");
                } else {
                    System.out.println("⚠ Crime ID not found.");
                }
            } catch (Exception e) {
                System.out.println("Error updating crime status: " + e.getMessage());
            }} catch (InputMismatchException e) {
            sc.nextLine();
            System.out.println("Invalid .Try again");
        }
    }

    void deleteCrime() {
        try{
            System.out.print("Enter Crime ID to delete: ");
            int crimeId = sc.nextInt();
            String sql = "DELETE FROM Crime WHERE crime_id = ?";
            try {
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setInt(1, crimeId);
                int deleted = pst.executeUpdate();
                if (deleted > 0) {
                    System.out.println("✅ Crime deleted successfully.");
                } else {
                    System.out.println("⚠ Crime ID not found.");
                }
            } catch (Exception e) {
                System.out.println("Error deleting crime: " + e.getMessage());
            }} catch (InputMismatchException e) {
            System.out.println("Invalid.Enter again");
        }
    }

    void viewAllUsers() {
        String sql = "SELECT u_id, u_name, u_mail FROM User";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            System.out.println("\n--- All Registered Users ---\n");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("User ID: " + rs.getInt("u_id"));
                System.out.println("Name: " + rs.getString("u_name"));
                System.out.println("Email: " + rs.getString("u_mail"));
                System.out.println("-------------------------");
            }
            if (!found) {
                System.out.println("No users registered yet.");
            }
        } catch (Exception e) {
            System.out.println("Error fetching users: " + e.getMessage());
        }
    }

    void deleteUser() {
        try{System.out.print("Enter User ID to delete: ");
            int userId = sc.nextInt();
            String sqlDeleteUser = "DELETE FROM User WHERE u_id = ?";
            String sqlDeleteUserCrimes = "DELETE FROM Crime WHERE u_id = ?";
            try {
                PreparedStatement pstCrimes = con.prepareStatement(sqlDeleteUserCrimes);
                pstCrimes.setInt(1, userId);
                pstCrimes.executeUpdate();

                PreparedStatement pstUser = con.prepareStatement(sqlDeleteUser);
                pstUser.setInt(1, userId);
                int deleted = pstUser.executeUpdate();

                if (deleted > 0) {
                    System.out.println("✅ User and their crime reports deleted successfully.");
                } else {
                    System.out.println("⚠ User ID not found.");
                }
            } catch (Exception e) {
                System.out.println("Error deleting user: " );
            } }catch (InputMismatchException e){
            sc.nextLine();
            System.out.println("Error: UserID is invalid.");
        }
    }
    void view_record_log() throws  Exception{
        String sql="select * from record_log";
        PreparedStatement pst=con.prepareStatement(sql);
        ResultSet rs=pst.executeQuery();
        while (rs.next()){
            System.out.println("log id is: "+rs.getInt(1));
            System.out.println("Crime id is: "+rs.getInt(2));
            System.out.println("user id is: "+rs.getInt(3));
            System.out.println("Description is: "+rs.getString(4));
            System.out.println("report Date is: "+rs.getDate(5));
            System.out.println("Action perform by user : "+rs.getString(6));
            System.out.println("Action perform at time : "+rs.getTime(7));
            System.out.println("Status of crime is: "+rs.getString(8));
        }
    }
}
class User extends  Admin {

    static int uid = 1;

    String e_mail;
    String pass;
    String uName;


    User(Connection con) {
        super(con);
    }

    void run(Connection con) throws Exception {
        this.con = con;
        String sql1 = "CREATE TABLE IF NOT EXISTS record_log (log_id INT AUTO_INCREMENT PRIMARY KEY,crime_id INT,user_id INT,description TEXT,report_date TIMESTAMP,action VARCHAR(20),log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,status VARCHAR(20) DEFAULT 'ACTIVE',remarks VARCHAR(255))";
        Statement st1 = con.createStatement();
        st1.executeUpdate(sql1);
        String sql2 = "CREATE TABLE IF NOT EXISTS User (u_id INT PRIMARY KEY AUTO_INCREMENT,u_name VARCHAR(50),u_mail VARCHAR(50) UNIQUE, u_pass VARCHAR(100))";
        Statement st2 = con.createStatement();
        st2.executeUpdate(sql2);
        String sql3 = "CREATE TABLE IF NOT EXISTS Crime (crime_id INT PRIMARY KEY AUTO_INCREMENT,u_id INT,crime_type VARCHAR(100),description text,location VARCHAR(100),date_reported DATE,status VARCHAR(20) DEFAULT 'Pending',FOREIGN KEY (u_id) REFERENCES User(u_id))";
        Statement st3 = con.createStatement();
        st3.executeUpdate(sql3);
        ds = new CrimeDataManager(con);
        ds.set_password_Array();
        ds.set_record_HashMap();
        ds.set_cid();
        ds.set_crime_record();
        System.out.println("👉 What would you like to do?\n1️⃣ - Log-In \n2️⃣ - Sign-Up ");
        try{
            System.out.print("Enter your choice: ");
            int choice1 = sc.nextInt();
            switch (choice1) {
                case 1:
                    log_in();
                    break;
                case 2:
                    sign_up();
                    break;
                default:
                    System.out.println("Please Enter from the option");
            }}catch(InputMismatchException e){
            sc.nextLine();
            System.out.println("Invalid choice");
        }
    }

    void log_in() {
        try {
            boolean b = true;
            int log_uid = 0;
            String log_email = "";
            String log_pass = "";
            String check_email = "";
            String check_pass = "";
            while (b) {
//if we want to use DS then ask user for uid and then use records.contains to check that mail and password are in same user account or not
                System.out.println("Enter your U_id: ");
                log_uid = sc.nextInt();
                System.out.println("Enter Your E-Mail ID:");
                log_email = sc.next();
                System.out.println("Enter Your 6-Digit Password:");
                log_pass = sc.next();
                if (log_email.contains("@gmail.com") || log_email.contains("@yahoo.com") && e_mail.length() > 10 && (log_pass.length() == 6) && (log_pass.contains("0") || log_pass.contains("1") || log_pass.contains("2") || log_pass.contains("3") || log_pass.contains("4") || log_pass.contains("5") || log_pass.contains("6") || log_pass.contains("7") || log_pass.contains("8") || log_pass.contains("9"))) {
                    b = false;
                    break;
                } else {
                    System.out.println("Please Enter Valid");
                }
            }
            b = ds.chek_email_exits(log_email);
            if (b) {
                String sql3 = "select u_mail,u_pass from user where u_id=?";
                PreparedStatement pst = con.prepareStatement(sql3);
                pst.setInt(1, log_uid);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    check_email = rs.getString("u_mail");
                    check_pass = rs.getString("u_pass");
                }
                if ((log_email.equals(check_email)) && (log_pass.equals(check_pass))) {
                    this.uid = log_uid;
                    System.out.println("log in successfully");
                    option();
                } else {
                    System.out.println("incorrect email id or password");
                }
            } else {
                System.out.println("From this e-mail you don't have any account!!");
            }
        } catch (Exception e) {
            System.out.println("Error in log_in");
        }
    }

    void sign_up() {
        try {
            boolean b = true;

            while (b) {
                System.out.println("Enter Your E-Mail ID:");
                this.e_mail = sc.next();
                if (e_mail.contains("@gmail.com") || e_mail.contains("@yahoo.com") && e_mail.length() > 10) {
                    b = false;
                    break;
                } else {
                    System.out.println("Please Enter Valid");
                }
            }
            b = ds.chek_email_exits(e_mail);
            if (b) {
                System.out.println("you have already created account from this e_mail");
            } else {
                b = true;
                while (b) {
                    System.out.println("Create Your 6-Digit Password:");
                    this.pass = sc.next();
                    if (pass.length() == 6 && (pass.contains("0") || pass.contains("1") || pass.contains("2") || pass.contains("3") || pass.contains("4") || pass.contains("5") || pass.contains("6") || pass.contains("7") || pass.contains("8") || pass.contains("9"))) {
                        boolean chek=ds.chek_pass(pass);
                        if (chek) {
                            System.out.println("Enter your name");
                            this.uName = sc.next();
                            String sql3 = "INSERT INTO User(u_name, u_mail, u_pass) VALUES (?, ?, ?)";
                            PreparedStatement pst3 = con.prepareStatement(sql3);
                            // pst3.setInt(1, uid);
                            pst3.setString(1, uName);
                            pst3.setString(2, e_mail);
                            pst3.setString(3, pass);
                            pst3.executeUpdate();
                            String sql4 = "select u_id from user where u_mail=?";
                            PreparedStatement pst4 = con.prepareCall(sql4);
                            pst4.setString(1, e_mail);
                            ResultSet rs = pst4.executeQuery();
                            while (rs.next()) {
                                int give_uid = rs.getInt(1);
                                System.out.println("your user id is:" + give_uid);
                                System.out.println("Your Account is created");
                                this.uid = give_uid;
                                option();
                            }
                            b = false;
                            break;
                        }
                        else {
                            System.out.println("This password can not be created Try another password!");
                        }
                    } else {
                        System.out.println("Kindly Create Only 6-Digit password");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error sign_up: ");
        }
    }

    void option() throws Exception {
        System.out.println();
        boolean b = true;
        while (b) {
            System.out.println();
            System.out.println("👉 Enter Your choice:");
            System.out.println("1️⃣ - Report a New Crime ");
            System.out.println("2️⃣ - View My Reported Crime ");
            System.out.println("3️⃣ - Update Crime Report ");
            System.out.println("4️⃣ - Delete Reported Crime ");
            System.out.println("5️⃣ - Get Statement copy");
            System.out.println("6️⃣ - Log Out ");
            int choice3 = sc.nextInt();
            switch (choice3) {
                case 1:
                    report_crime();
                    break;
                case 2:
                    view_my_report();
                    break;
                case 3:
                    update_crime();
                    break;
                case 4:
                    delete_crime();
                    break;
                case 5:
                    get_copy();
                    break;
                case 6:
                    b = false;
                    break;
            }
        }
    }

    void report_crime() {
        try {
            sc.nextLine();
            String crimeType = "";
            boolean isValid = false;

            while (!isValid) {
                try {
                    System.out.println("Select Crime Type from below options:");
                    System.out.println("1 - Theft");
                    System.out.println("2 - Assault");
                    System.out.println("3 - Fraud");
                    System.out.println("4 - Vandalism");
                    System.out.println("5 - Other");
                    System.out.print("Enter your choice (1-5): ");

                    int choice = sc.nextInt();
                    sc.nextLine();

                    switch (choice) {
                        case 1 -> crimeType = "Theft";
                        case 2 -> crimeType = "Assault";
                        case 3 -> crimeType = "Fraud";
                        case 4 -> crimeType = "Vandalism";
                        case 5 -> {
                            System.out.print("Enter other crime type: ");
                            crimeType = sc.nextLine();
                        }
                        default -> {
                            System.out.println("Invalid choice. Please enter a number between 1 and 5.");
                            continue;
                        }
                    }
                    isValid = true; // valid choice made, exit loop
                } catch (InputMismatchException e) {
                    System.out.println("Invalid input type! Please enter a numeric value between 1 and 5.");
                    sc.nextLine();
                }
            }

            System.out.println("Enter Statement For Crime:");
            String statement = sc.nextLine();

            System.out.println("Enter Location Of The Crime:");
            String location = sc.nextLine();

            String status = "pending";
            String sql2 = "insert into crime(u_id, crime_type, description, location, date_reported, status) values(?, ?, ?, ?, curdate(), ?)";

            PreparedStatement st2 = con.prepareStatement(sql2);
            st2.setInt(1, this.uid);
            st2.setString(2, crimeType);
            st2.setString(3, statement);
            st2.setString(4, location);
            st2.setString(5, status);

            st2.executeUpdate();

            System.out.println("Crime reported successfully for type: " + crimeType);

        } catch (Exception e) {
            System.out.println("Error reporting crime: " + e.getMessage());
        }
    }


    void view_my_report() {
        try {
            String sql = "select * from crime where u_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, this.uid);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                System.out.println("Crime ID: " + rs.getInt("crime_id"));
                System.out.println("Type: " + rs.getString("crime_type"));
                System.out.println("Description: " + rs.getString("description"));
                System.out.println("Location: " + rs.getString("location"));
                System.out.println("Date: " + rs.getDate("date_reported"));
                System.out.println("Status: " + rs.getString("status"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching crimes: " + e.getMessage());
        }
    }

    void update_crime() {
        System.out.println("Enter Crime ID to update:");
        int crime_id = sc.nextInt();
        sc.nextLine();
        System.out.println("Enter new description:");
        String new_desc = sc.nextLine();
        String sql = "UPDATE Crime SET description = ? WHERE crime_id = ? AND u_id = ?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, new_desc);
            pst.setInt(2, crime_id);
            pst.setInt(3, this.uid);
            int rows = pst.executeUpdate();
            if (rows > 0)
                System.out.println("Crime updated successfully.");
            else
                System.out.println("No matching record found.");
        } catch (Exception e) {
            System.out.println("Error updating crime: " + e.getMessage());
        }
    }

    void delete_crime() throws Exception {
        con.setAutoCommit(false);
        System.out.println("Enter Crime id of report which you want to deete");
        int cid_del = sc.nextInt();
        String sql = "delete from crime where crime_id=? and u_id=?";
        try {
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, cid_del);
            pst.setInt(2, this.uid);
            int r = pst.executeUpdate();
            if (r > 0) {
                System.out.println("You want to delete your complain?(Yes Or No)");
                String ans = sc.next();
                if (ans.equalsIgnoreCase("yes")) {
                    con.commit();
                    con.setAutoCommit(true);
                    System.out.println("Your complain is deleted");
                }
                else if (ans.equalsIgnoreCase("no")) {
                    con.rollback();
                    con.setAutoCommit(true);
                }
                else
                {
                    System.out.println("Give your answer in (yes or no)!");
                }
            } else {
                System.out.println(" No such crime found under your account." + this.uid);
            }
        } catch (Exception e) {
            System.out.println("Error deleting crime: " + e.getMessage());
        }
    }

    void get_copy() {
        try {
            System.out.println("Enter Crime id");
            int cid = sc.nextInt();
            String type = "";
            Date date = null;
            boolean b = ds.check_cid(cid,this.uid);
            if (b) {
                String sql1 = "select crime_type,date_reported from crime where crime_id=?";
                PreparedStatement pst = con.prepareStatement(sql1);
                pst.setInt(1, cid);
                ResultSet rs1 = pst.executeQuery();
                while (rs1.next()) {
                    type = rs1.getString("crime_type");
                    date = rs1.getDate("date_reported");
                }
                String sql2 = "select description from crime where crime_id=?";
                PreparedStatement pst2 = con.prepareStatement(sql2);
                pst2.setInt(1, cid);
                ResultSet rs2 = pst2.executeQuery();
                FileWriter fw = new FileWriter(type + "_" + cid + "_" + date + ".txt");
                while (rs2.next()) {
                    Reader r = rs2.getCharacterStream(1);
                    int i;
                    while ((i = r.read()) != -1) {
                        fw.write(i);
                    }
                }
                fw.close();
                System.out.println("Your copy of statement is in the dashboard.");
                System.out.println(type + "_" + cid + "_" + date + ".txt");
            }
            else
                System.out.println("No crime Reported from crime id "+cid);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
class OfficerPanel extends Admin{
    OfficerPanel(Connection con) {
        super(con);
    }

    // Officer Menu
    void run() {
        try{
            this.con = con;
            boolean running = true;
            while (running) {
                System.out.println("\n👮 === Officer Panel === 👮");
                System.out.println("1️⃣ - View All Assigned Crimes ");
                System.out.println("2️⃣ - Update Crime Status ");
                System.out.println("3️⃣ - Log Out ");
                System.out.print("👉 Enter your choice: ");

                int choice = sc.nextInt();

                switch (choice) {
                    case 1 -> viewAssignedCrimes();
                    case 2 -> updateCrimeStatus();
                    case 3 -> {
                        running = false;
                        System.out.println("👋 Logging out Officer...");
                    }
                    default -> System.out.println("⚠ Invalid choice.");
                }
            }
        }catch (InputMismatchException e) {
            sc.nextLine();
            System.out.println("Invalid choice.");
        }
    }

    // Show crimes assigned to this officer
    void viewAssignedCrimes() {
        try {
            System.out.print("Enter your Officer ID: ");
            int officerId;
            try {
                officerId = sc.nextInt();
            } catch (InputMismatchException ime) {
                System.out.println("⚠ Invalid choice. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
                return; // stop execution of this method
            }

            String sql = "SELECT c.crime_id, c.crime_type, c.description, c.location, c.date_reported, c.status " +
                    "FROM Crime c JOIN Crime_Officer_Assign coa ON c.crime_id = coa.crime_id " +
                    "WHERE coa.officer_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, officerId);
            ResultSet rs = pst.executeQuery();

            System.out.println("\n--- Crimes Assigned To You ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("Crime ID: " + rs.getInt("crime_id"));
                System.out.println("Type: " + rs.getString("crime_type"));
                System.out.println("Description: " + rs.getString("description"));
                System.out.println("Location: " + rs.getString("location"));
                System.out.println("Date Reported: " + rs.getDate("date_reported"));
                System.out.println("Status: " + rs.getString("status"));
                System.out.println("-------------------------");
            }
            if (!found) {
                System.out.println("⚠ No crimes assigned yet.");
            }
        } catch (Exception e) {
            System.out.println("⚠ Error fetching assigned crimes.");
        }
    }


    // Officer can update status of assigned crime
    void updateCrimeStatus() {
        try {
            int officerId;
            System.out.print("Enter your Officer ID: ");
            try {
                officerId = sc.nextInt();
            } catch (InputMismatchException ime) {
                System.out.println("⚠ Invalid choice. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
                return;
            }

            int crimeId;
            System.out.print("Enter Crime ID to update: ");
            try {
                crimeId = sc.nextInt();
            } catch (InputMismatchException ime) {
                System.out.println("⚠ Invalid choice. Please enter a valid number.");
                sc.nextLine(); // clear invalid input
                return;
            }

            sc.nextLine(); // clear buffer
            System.out.print("Enter new status (In Progress / Resolved): ");
            String status = sc.nextLine();

            String sqlCheck = "SELECT COUNT(*) FROM Crime_Officer_Assign WHERE crime_id = ? AND officer_id = ?";
            PreparedStatement pstCheck = con.prepareStatement(sqlCheck);
            pstCheck.setInt(1, crimeId);
            pstCheck.setInt(2, officerId);
            ResultSet rsCheck = pstCheck.executeQuery();
            rsCheck.next();

            if (rsCheck.getInt(1) == 0) {
                System.out.println("⚠ You are not assigned to this crime.");
                return;
            }

            String sql = "UPDATE Crime SET status = ? WHERE crime_id = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, status);
            pst.setInt(2, crimeId);

            int updated = pst.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Crime status updated successfully.");
            } else {
                System.out.println("⚠ Crime ID not found.");
            }
        } catch (Exception e) {
            System.out.println("⚠ Error updating crime status.");
        }
    }

}

class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String dburl = "jdbc:mysql://localhost:3306/crime_management";
        String dbuser = "root";
        String dbpass = "";
        String Drivername = "com.mysql.cj.jdbc.Driver";
        Class.forName(Drivername);
        Connection con = DriverManager.getConnection(dburl, dbuser, dbpass);
        boolean b = true;
        Admin a = new Admin(con);
        User u = new User(con);
        OfficerPanel o = new OfficerPanel(con);
        while (b) {
            System.out.println("\n=========================================================");
            System.out.println("\n          🚨🚔 CRIME MANAGEMENT SYSTEM 🕵‍♂🔒          ");
            System.out.println("\n=========================================================");
            System.out.println(" ✨🌟Welcome to the Crime Management System🌟✨\nLet's make our country crime free.");
            System.out.println("Please select your role to continue:");
            System.out.println("1️⃣  - Join as an User ");
            System.out.println("2️⃣  - Join as an Admin ");
            System.out.println("3️⃣  - Join as an Officer ");
            System.out.println("4️⃣  - Exit ❌");
            System.out.print("👉 Enter Your Choice: ");
            String input = sc.next();

            switch (input) {
                case "1":
                    u.run(con);
                    break;
                case "2":
                    a.run(con);
                    break;
                case "3":
                    o.run();
                    break;
                case "4":
                    System.out.println("Thank You for your valuable time.");
                    b = false;
                    break;
                default:
                    System.out.println("⚠ Please enter only integer between 1 to 4");
            }
        }
    }
}