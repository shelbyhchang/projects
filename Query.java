package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Added this for java.sql.Types.INTEGER
import java.sql.Types;

/**
 * This file is from my coursework in CSE 344 at the University of Washington. It is a part of a Flights Application that we built.
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  // clearTables() statements
  private static final String CLEAR_USER_SQL = "DELETE FROM USER_schang01";
  private PreparedStatement clearUserStmt;

  private static final String CLEAR_RES_SQL = "DELETE FROM RESERVATION_schang01";
  private PreparedStatement clearResStmt;

  // transaction_createCustomer() statements
  private static final String CREATE_USER_SQL = "INSERT INTO USER_schang01 VALUES(?, ?, ?)";
  private PreparedStatement createUserStmt;

  // transaction_login() statements
  private static final String CHECK_LOGIN_SQL = "SELECT * FROM USER_schang01 WHERE username = ?";
  private PreparedStatement checkLoginStmt;

  // transaction_search() statements
  private static final String SEARCH_ONE_HOP_SQL = "SELECT TOP (?) "
        + "fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
        + "FROM Flights " + "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? "
        + "AND canceled = 0 ORDER BY actual_time ASC";
  private PreparedStatement searchOneHopStmt;

  private static final String SEARCH_MAX_TWO_HOP_SQL = "SELECT * FROM ( "
        + "SELECT TOP (?) * "
        + "FROM ("
        + "SELECT fid AS F1_fid, day_of_month AS F1_day_of_month, carrier_id AS F1_carrier_id, flight_num AS F1_flight_num, "
        + "origin_city AS F1_origin_city, dest_city AS F1_dest_city, actual_time AS F1_actual_time, capacity AS F1_capacity, price AS F1_price, "
        + "NULL AS F2_fid, NULL AS F2_day_of_month, NULL AS F2_carrier_id, NULL AS F2_flight_num, "
        + "NULL AS F2_origin_city, NULL AS F2_dest_city, NULL AS F2_actual_time, NULL AS F2_capacity, NULL AS F2_price, NULL AS indirect "
        + "FROM FLIGHTS "
        + "WHERE canceled = 0 AND origin_city = ? AND dest_city = ? AND day_of_month = ? "
        + "UNION "
        + "SELECT F1.fid AS F1_fid, F1.day_of_month AS F1_day_of_month, F1.carrier_id AS F1_carrier_id, F1.flight_num AS F1_flight_num, "
        + "F1.origin_city AS F1_origin_city, F1.dest_city AS F1_dest_city, F1.actual_time AS F1_actual_time, F1.capacity AS F1_capacity, F1.price AS F1_price, "
        + "F2.fid AS F2_fid, F2.day_of_month AS F2_day_of_month, F2.carrier_id AS F2_carrier_id, F2.flight_num AS F2_flight_num, "
        + "F2.origin_city AS F2_origin_city, F2.dest_city AS F2_dest_city, F2.actual_time AS F2_actual_time, F2.capacity AS F2_capacity, F2.price AS F2_price, "
        + "1 AS indirect "
        + "FROM FLIGHTS AS F1, FLIGHTS AS F2 "
        + "WHERE F1.canceled = 0 AND F2.canceled = 0 "
        + "AND F1.origin_city = ? AND F2.dest_city = ? AND F1.dest_city = F2.origin_city "
        + "AND F1.day_of_month = ? AND F2.day_of_month = ? "
        + ") AS COMBINED ORDER BY indirect, (F1_actual_time + ISNULL(F2_actual_time, 0)), F1_fid, F2_fid"
        + ") AS SELECTED_FLIGHTS ORDER BY (F1_actual_time + ISNULL(F2_actual_time, 0)), F1_fid, F2_fid";
  private PreparedStatement searchMaxTwoHopStmt;

  // transaction_book() statements
  private static final String CHECK_FLIGHT1_CAPACITY_SQL = "SELECT COUNT(*) AS count FROM RESERVATION_schang01 WHERE "
        + "fid1 = ? OR fid2 = ?"; 
  private PreparedStatement checkFlight1CapacityStmt;

  private static final String CHECK_FLIGHT2_CAPACITY_SQL = "SELECT COUNT(*) AS count FROM RESERVATION_schang01 WHERE "
        + "fid1 = ? OR fid2 = ?"; 
  private PreparedStatement checkFlight2CapacityStmt;
  
  private static final String ADD_RESERVATION_SQL = "INSERT INTO RESERVATION_schang01 VALUES(?, ?, ?, ?, ?)";
  private PreparedStatement addReservationStmt;

  private static final String GET_NUM_RESERVATIONS_SQL = "SELECT COUNT(*) AS count FROM RESERVATION_schang01";
  private PreparedStatement getNumReservationsStmt;

  private static final String CHECK_ALREADY_BOOKED_SQL = "SELECT COUNT(*) AS count FROM RESERVATION_schang01 r, "
        + "FLIGHTS f WHERE r.fid1 = f.fid AND r.userID = ? AND f.day_of_month = ?";
  private PreparedStatement checkAlreadyBookedStmt;

  // transaction_pay() statements
  private static final String CHECK_USER_RES_SQL = "SELECT COUNT(*) AS count FROM RESERVATION_schang01 WHERE userID = ?";
  private PreparedStatement checkUserResStmt;
  
  private static final String CHECK_BALANCE_SQL = "SELECT balance FROM USER_schang01 WHERE username = ?";
  private PreparedStatement checkBalanceStmt;

  private static final String GET_PRICE_SQL = "SELECT price FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement getPriceStmt;

  private static final String UPDATE_BALANCE_SQL = "UPDATE USER_schang01 SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalanceStmt;

  private static final String UPDATE_RES_TO_PAID_SQL = "UPDATE RESERVATION_schang01 SET paid = 1 WHERE "
        + "userID = ? AND resID = ?";
  private PreparedStatement updateResToPaidStmt;

  private static final String CHECK_ALREADY_PAID_SQL = "SELECT paid, fid1, fid2 FROM RESERVATION_schang01 WHERE resID = ?";
  private PreparedStatement checkAlreadyPaidStmt;

  // transaction_reservations() statements
  private static final String GET_USER_RES_SQL = "SELECT * FROM RESERVATION_schang01 WHERE userID = ? ORDER BY resID";
  private PreparedStatement getUserResStmt;

  private static final String GENERATE_FLIGHT_INFO_SQL = "SELECT * FROM FLIGHTS WHERE fid = ?";
  private PreparedStatement generateFlightInfoStmt;


  //
  // Instance variables
  //
  private boolean loggedIn;
  private String userID;
  private boolean searched;
  private int recentNumItin;
  private String recentSearch;
  private int itinDay;
  private int itinFid1;
  private int itinCap1;
  private int itinPrice1;
  private int itinFid2;
  private int itinCap2;
  private int itinPrice2;

  protected Query() throws SQLException, IOException {
    loggedIn = false;
    userID = "";
    searched = false;
    recentNumItin = -1;
    recentSearch = "";
    itinDay = 0;
    itinFid1 = 0;
    itinCap1 = 0;
    itinPrice1 = 0;
    itinFid2 = 0;
    itinCap2 = 0;
    itinPrice2 = 0;
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      clearResStmt.executeUpdate();
      clearUserStmt.executeUpdate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    // TODO: YOUR CODE HERE
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    clearUserStmt = conn.prepareStatement(CLEAR_USER_SQL);
    clearResStmt = conn.prepareStatement(CLEAR_RES_SQL);
    createUserStmt = conn.prepareStatement(CREATE_USER_SQL);
    checkLoginStmt = conn.prepareStatement(CHECK_LOGIN_SQL);
    searchOneHopStmt = conn.prepareStatement(SEARCH_ONE_HOP_SQL);
    searchMaxTwoHopStmt = conn.prepareStatement(SEARCH_MAX_TWO_HOP_SQL);
    addReservationStmt = conn.prepareStatement(ADD_RESERVATION_SQL);
    checkFlight1CapacityStmt = conn.prepareStatement(CHECK_FLIGHT1_CAPACITY_SQL);
    checkFlight2CapacityStmt = conn.prepareStatement(CHECK_FLIGHT2_CAPACITY_SQL);
    checkAlreadyBookedStmt = conn.prepareStatement(CHECK_ALREADY_BOOKED_SQL);
    checkUserResStmt = conn.prepareStatement(CHECK_USER_RES_SQL);
    checkBalanceStmt = conn.prepareStatement(CHECK_BALANCE_SQL);
    getPriceStmt = conn.prepareStatement(GET_PRICE_SQL);
    updateBalanceStmt = conn.prepareStatement(UPDATE_BALANCE_SQL);
    updateResToPaidStmt = conn.prepareStatement(UPDATE_RES_TO_PAID_SQL);
    getNumReservationsStmt = conn.prepareStatement(GET_NUM_RESERVATIONS_SQL);
    checkAlreadyPaidStmt = conn.prepareStatement(CHECK_ALREADY_PAID_SQL);
    getUserResStmt = conn.prepareStatement(GET_USER_RES_SQL);
    generateFlightInfoStmt = conn.prepareStatement(GENERATE_FLIGHT_INFO_SQL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n".  For all
   *         other errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    
    try {
      // Check if someone has already logged in
      if (loggedIn) {
        return "User already logged in\n";
      } 

      try {
        String formatUsername = username.toUpperCase();
        byte[] pwd = PasswordUtils.hashPassword(password);
        
        // Set parameters and execute the SQL query
        checkLoginStmt.clearParameters();
        checkLoginStmt.setString(1, formatUsername);
        ResultSet rs = checkLoginStmt.executeQuery();

        String resultUsername = "";
        byte[] resultPassword = new byte[144];

        while (rs.next()) {
          // If SQL query output has 1(+) row, acquire the username and password
          resultUsername = rs.getString(1);
          resultPassword = rs.getBytes(2);
        }

        rs.close();
        // Check if the resulting username and password match those inputted by the user
        if (resultUsername.equals(formatUsername) && PasswordUtils.plaintextMatchesHash(password, resultPassword)) {
          loggedIn = true;
          userID = username;
          return "Logged in as " + username + "\n";
        }
      } catch (SQLException e) {
        return "Login failed\n";
      }

      return "Login failed\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE
    
    try {
      // Check for negative initial balance
      if (initAmount < 0) {
        return "Failed to create user\n";
      }
      
      try {
        String formatUsername = username.toUpperCase();
        byte[] pwd = PasswordUtils.hashPassword(password); // Generate salted hashed password to be stored
        
        // Set parameters for SQL query and execute
        createUserStmt.clearParameters();
        createUserStmt.setString(1, formatUsername);
        createUserStmt.setBytes(2, pwd);
        createUserStmt.setInt(3, initAmount);
        createUserStmt.executeUpdate();

        return "Created user " + username + "\n";
      } catch (SQLException e) {        
        return "Failed to create user\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given
   * day of the month. If {@code directFlight} is true, it only searches for direct flights,
   * otherwise is searches for direct flights and flights with two "hops." Only searches for up
   * to the number of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

    try {
      StringBuffer sb = new StringBuffer();

      // If invalid number of itineraries, same origin and destination cities, or invalid day of month, then return.
      if (numberOfItineraries < 1 || originCity.equals(destinationCity)) {
        return "Failed to search\n";
      } else if (dayOfMonth < 1 || dayOfMonth > 31) {
        return "Failed to search\n";
      }

      try {
        ResultSet results;
        int itinCounter = 0;
        Flight newFlight1, newFlight2;

        if (directFlight) {
          // One hop itineraries

          // Set parameters and execute SQL query to find direct flight itineraries
          searchOneHopStmt.clearParameters();
          searchOneHopStmt.setInt(1, numberOfItineraries);
          searchOneHopStmt.setString(2, originCity);
          searchOneHopStmt.setString(3, destinationCity);
          searchOneHopStmt.setInt(4, dayOfMonth);

          results = searchOneHopStmt.executeQuery();

          // Check if query result has zero tuples
          if (!results.next()) {
            searched = false;
            recentSearch = ""; 
            return "No flights match your selection\n";
          } else {
            newFlight1 = formatFlight(results.getInt("fid"), results.getInt("day_of_month"), results.getString("carrier_id"),
                                            results.getString("flight_num"), results.getString("origin_city"), results.getString("dest_city"),
                                            results.getInt("actual_time"), results.getInt("capacity"), results.getInt("price"));
            
            sb.append(formatDirectFlightItinerary(itinCounter, newFlight1));
            itinCounter++;
          }
          
          // Continue formatting/outputting flight itineraries for all resulting tuples
          while (results.next() && itinCounter < numberOfItineraries) {
            newFlight1 = formatFlight(results.getInt("fid"), results.getInt("day_of_month"), results.getString("carrier_id"),
                                            results.getString("flight_num"), results.getString("origin_city"), results.getString("dest_city"),
                                            results.getInt("actual_time"), results.getInt("capacity"), results.getInt("price"));
            
            sb.append(formatDirectFlightItinerary(itinCounter, newFlight1));
            itinCounter++;
          }
        } else {
          // Max two hop itineraries

          // Set parameters and execute SQL query to find direct/indirect flight itineraries
          searchMaxTwoHopStmt.clearParameters();
          searchMaxTwoHopStmt.setInt(1, numberOfItineraries);
          searchMaxTwoHopStmt.setString(2, originCity);
          searchMaxTwoHopStmt.setString(3, destinationCity);
          searchMaxTwoHopStmt.setInt(4, dayOfMonth);
          searchMaxTwoHopStmt.setString(5, originCity);
          searchMaxTwoHopStmt.setString(6, destinationCity);
          searchMaxTwoHopStmt.setInt(7, dayOfMonth);
          searchMaxTwoHopStmt.setInt(8, dayOfMonth);
          
          results = searchMaxTwoHopStmt.executeQuery();

          // Check if query result has zero tuples
          if (!results.next()) {
            return "No flights match your selection\n";
          } else {
            newFlight1 = formatFlight(results.getInt(1), results.getInt(2), results.getString(3),
                                            results.getString(4), results.getString(5), results.getString(6),
                                            results.getInt(7), results.getInt(8), results.getInt(9));

            // Get the "fid" of the second flight (if exists)
            int result_fid2 = results.getInt(10);
            
            // Check if the "fid" of the second flight is 0, indicating a direct flight, then append accordingly
            if (result_fid2 == 0) {
              sb.append(formatDirectFlightItinerary(itinCounter, newFlight1));
            } else {
              newFlight2 = formatFlight(results.getInt(10), results.getInt(11), results.getString(12),
                                            results.getString(13), results.getString(14), results.getString(15),
                                            results.getInt(16), results.getInt(17), results.getInt(18));
              sb.append(formatIndirectFlightItinerary(itinCounter, newFlight1, newFlight2));
            }
            itinCounter++; // Update the number of itineraries found
          }
          
          // Continue formatting/outputting flight itineraries for all resulting tuples
          while (results.next() && itinCounter < numberOfItineraries) {
            newFlight1 = formatFlight(results.getInt(1), results.getInt(2), results.getString(3),
                                            results.getString(4), results.getString(5), results.getString(6),
                                            results.getInt(7), results.getInt(8), results.getInt(9));

            // Get the "fid" of the second flight (if exists)
            int result_fid2 = results.getInt(10);
            
            // Check if the "fid" of the second flight is 0, indicating a direct flight, then append accordingly
            if (result_fid2 == 0) {
              sb.append(formatDirectFlightItinerary(itinCounter, newFlight1));
            } else {
              newFlight2 = formatFlight(results.getInt(10), results.getInt(11), results.getString(12),
                                            results.getString(13), results.getString(14), results.getString(15),
                                            results.getInt(16), results.getInt(17), results.getInt(18));
              sb.append(formatIndirectFlightItinerary(itinCounter, newFlight1, newFlight2));
            }
            itinCounter++; // Update the number of itineraries found
          }
        }

        results.close();
        if (loggedIn) {
          // Only update these variables if user is logged in first
          recentNumItin = itinCounter - 1;
          searched = true;
          recentSearch = sb.toString();
        }
        return sb.toString();
      } catch (SQLException e) {
        return "Failed to search\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  // Private helper method to format a flight, given output from SQL query
  private Flight formatFlight(int fid, int day, String cid, String flightNum, String origin, String dest, int time, int capacity, int price) {
    return new Flight(fid, day, cid, flightNum, origin, dest, time, capacity, price);
  }

  // Private helper method to format a flight itinerary for a direct flight
  private String formatDirectFlightItinerary(int itinCounter, Flight flight1) {
    return "Itinerary " + itinCounter + ": 1 flight(s), " + flight1.time + " "
            + "minutes\n" + flight1.toString() + "\n";
  }

  // Private helper method to format a flight itinerary for an indirect itinerary (2 flights)
  private String formatIndirectFlightItinerary(int itinCounter, Flight flight1, Flight flight2) {
    int totalTime = flight1.time + flight2.time;
    return "Itinerary " + itinCounter + ": 2 flight(s), " + totalTime + " "
            + "minutes\n" + flight1.toString() + "\n" + flight2.toString() + "\n";
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search
   *                    in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged
   *         in\n". If the user is trying to book an itinerary with an invalid ID or without
   *         having done a search, then return "No such itinerary {@code itineraryId}\n". If the
   *         user already has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same day\n". For all
   *         other errors, return "Booking failed\n".
   *
   *         If booking succeeds, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from
   *         1 and increments by 1 each time a successful reservation is made by any user in
   *         the system.
   */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    try {
      // Check if someone has already logged in
      if (!loggedIn) {
        return "Cannot book reservations, not logged in\n";
      } 

      if (!searched || itineraryId < 0 || itineraryId > recentNumItin) {
        return "No such itinerary " + itineraryId + "\n";
      }

      getItinerary(itineraryId);
      int desiredDay = itinDay;
      int resId = 0;

      boolean deadlock = true;
      while (deadlock) {
        deadlock = false;
        try {
          conn.setAutoCommit(false);

          checkAlreadyBookedStmt.clearParameters();
          checkAlreadyBookedStmt.setString(1, userID.toUpperCase());
          checkAlreadyBookedStmt.setInt(2, desiredDay);
          ResultSet rs = checkAlreadyBookedStmt.executeQuery();
          rs.next();
          if (rs.getInt("count") > 0) {
            rs.close();
            conn.rollback();
            conn.setAutoCommit(true);
            return "You cannot book two flights in the same day\n";
          }
          
          // Check if flight 1 has reached capacity
          checkFlight1CapacityStmt.clearParameters();
          checkFlight1CapacityStmt.setInt(1, itinFid1);
          checkFlight1CapacityStmt.setInt(2, itinFid1);
          rs = checkFlight1CapacityStmt.executeQuery();
          rs.next();
          if ((itinCap1 - rs.getInt("count")) <= 0) {
            rs.close();
            conn.rollback();
            conn.setAutoCommit(true);
            return "Booking failed\n";
          }

          // Check if flight 2 (if exists) has reached capacity
          if (itinFid2 != 0) {
            checkFlight2CapacityStmt.clearParameters();
            checkFlight2CapacityStmt.setInt(1, itinFid2);
            checkFlight2CapacityStmt.setInt(2, itinFid2);
            rs = checkFlight2CapacityStmt.executeQuery();
            rs.next();
            if ((itinCap2 - rs.getInt("count")) <= 0) {
              rs.close();
              conn.rollback();
              conn.setAutoCommit(true);
              return "Booking failed\n";
            }
          }

          // Set parameters for addReservationStmt
          rs = getNumReservationsStmt.executeQuery();
          rs.next();
          resId = rs.getInt("count") + 1;
          rs.close();


          addReservationStmt.clearParameters();
          addReservationStmt.setInt(1, resId);
          addReservationStmt.setString(2, userID.toUpperCase());
          addReservationStmt.setInt(3, 0);
          addReservationStmt.setInt(4, itinFid1);

          if (itinFid2 == 0) {
            addReservationStmt.setNull(5, Types.INTEGER);
          } else {
            addReservationStmt.setInt(5, itinFid2);
          }

          // Execute query
          addReservationStmt.executeUpdate();
          conn.commit();
          conn.setAutoCommit(true);
          return "Booked flight(s), reservation ID: " + resId + "\n";
        } catch (SQLException e) {
          deadlock = isDeadlock(e);
          if (deadlock) {
            try {
              conn.rollback();
              conn.setAutoCommit(true);
            } catch (SQLException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
      return "Booking failed\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  // Private helper method to parse through most recent search for the entire itinerary
  private void getItinerary(int itineraryId) {
    int itinIndex = recentSearch.indexOf("Itinerary " + itineraryId);
    int numFlightsIndex = recentSearch.indexOf(": ", itinIndex) + 2;

    int day1Start = recentSearch.indexOf("Day: ", itinIndex) + 5;
    int day1End =  recentSearch.indexOf(" ", day1Start);
    String day1 = recentSearch.substring(day1Start, day1End);
    itinDay = Integer.parseInt(day1);

    int fid1Start = recentSearch.indexOf("ID:", itinIndex) + 4;
    int fid1End = recentSearch.indexOf(" ", fid1Start);
    String fid1 = recentSearch.substring(fid1Start, fid1End);
    itinFid1 = Integer.parseInt(fid1);

    int cap1Start = recentSearch.indexOf("Capacity: ", fid1End) + 10;
    int cap1End = recentSearch.indexOf(" ", cap1Start);
    String cap1 = recentSearch.substring(cap1Start, cap1End);
    itinCap1 = Integer.parseInt(cap1);

    int price1Start = recentSearch.indexOf("Price: ", cap1End) + 7;
    int price1End = recentSearch.indexOf("\n", price1Start);
    String price1 = recentSearch.substring(price1Start, price1End);
    itinPrice1 = Integer.parseInt(price1);
    
    // If the itinerary only has one flight, only parse for flight1
    if (recentSearch.substring(numFlightsIndex, numFlightsIndex + 1).equals("2")) {
      int fid2Start = recentSearch.indexOf("ID:", price1End) + 4;
      int fid2End = recentSearch.indexOf(" ", fid2Start);
      String fid2 = recentSearch.substring(fid2Start, fid2End);
      itinFid2 = Integer.parseInt(fid2);

      int cap2Start = recentSearch.indexOf("Capacity: ", fid2End) + 10;
      int cap2End = recentSearch.indexOf(" ", cap2Start);
      String cap2 = recentSearch.substring(cap2Start, cap2End);
      itinCap2 = Integer.parseInt(cap2);

      int price2Start = recentSearch.indexOf("Price: ", cap2End) + 7;
      int price2End = recentSearch.indexOf("\n", price2Start);
      String price2 = recentSearch.substring(price2Start, price2End);
      itinPrice2 = Integer.parseInt(price2);
    } 
  }
  
  
  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n". If the
   *         reservation is not found / not under the logged in user's name, then return
   *         "Cannot find unpaid reservation [reservationId] under user: [username]\n".  If
   *         the user does not have enough money in their account, then return
   *         "User has only [balance] in account but itinerary costs [cost]\n".  For all other
   *         errors, return "Failed to pay for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    try {
      if (!loggedIn) {
        return "Cannot pay, not logged in\n";
      }

      boolean deadlock = true;
      while (deadlock) {
        deadlock = false;
        try {
          conn.setAutoCommit(false);
          
          int remainingBalance = 0;

          checkUserResStmt.setString(1, userID.toUpperCase());
          ResultSet rs = checkUserResStmt.executeQuery();
          rs.next();
          if (rs.getInt("count") < reservationId) {
            rs.close();
            conn.commit();
            conn.setAutoCommit(true);
            return "Cannot find unpaid reservation " + reservationId + " under user: " + userID + "\n";
          }

          // Check if reservation has already been paid for
          checkAlreadyPaidStmt.clearParameters();
          checkAlreadyPaidStmt.setInt(1, reservationId);
          rs = checkAlreadyPaidStmt.executeQuery();
          rs.next();

          if (rs.getInt("paid") == 1) {
            rs.close();
            conn.commit();
            conn.setAutoCommit(true);
            return "Cannot find unpaid reservation " + reservationId + " under user: " + userID + "\n";
          }
          
          int totalPrice = 0;
          
          // Get the price of flight 1 of the reservation
          int fid1 = rs.getInt("fid1");
          int fid2 = rs.getInt("fid2");
          getPriceStmt.clearParameters();
          getPriceStmt.setInt(1, fid1);
          rs = getPriceStmt.executeQuery();
          rs.next();
          totalPrice += rs.getInt("price");
          
          // Update totalPrice if there is a second flight in the reservation
          if (fid2 != 0) {
            getPriceStmt.clearParameters();
            getPriceStmt.setInt(1, fid2);
            rs = getPriceStmt.executeQuery();
            rs.next();
            totalPrice += rs.getInt("price");
          }

          checkBalanceStmt.clearParameters();
          checkBalanceStmt.setString(1, userID.toUpperCase());
          rs = checkBalanceStmt.executeQuery();
          rs.next();
          int userBalance = rs.getInt("balance");
          rs.close();

          if (totalPrice > userBalance) {
            conn.commit();
            conn.setAutoCommit(true);
            return "User has only " + userBalance + " in account but itinerary costs " + totalPrice + "\n";
          }

          // Update the user's balance
          remainingBalance = userBalance - totalPrice;
          updateBalanceStmt.clearParameters();
          updateBalanceStmt.setInt(1, remainingBalance);
          updateBalanceStmt.setString(2, userID.toUpperCase());
          updateBalanceStmt.executeUpdate();

          // Update the paid field for the reservation
          updateResToPaidStmt.clearParameters();
          updateResToPaidStmt.setString(1, userID.toUpperCase());
          updateResToPaidStmt.setInt(2, reservationId);
          updateResToPaidStmt.executeUpdate();

          conn.commit();
          conn.setAutoCommit(true);
          return "Paid reservation: " + reservationId + " remaining balance: " + remainingBalance + "\n";
        } catch (SQLException e) {
          deadlock = isDeadlock(e);
          //if (deadlock) {
            try {
              conn.rollback();
              conn.setAutoCommit(true);
            } catch (SQLException e1) {
              e1.printStackTrace();
            }
          //}
        }
      }
      return "Failed to pay for reservation " + reservationId + "\n";
    } finally {
      checkDanglingTransaction();
    }
  }


  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE

    try {
      if (!loggedIn) {
        return "Cannot view reservations, not logged in\n";
      }

      boolean deadlock = true;
      while (deadlock) {
        try {
          conn.setAutoCommit(false);
          StringBuffer sb = new StringBuffer();

          getUserResStmt.clearParameters();
          getUserResStmt.setString(1, userID.toUpperCase());
          ResultSet rs = getUserResStmt.executeQuery();

          if (!rs.next()) {
            rs.close();
            conn.rollback();
            conn.setAutoCommit(true);
            return "No reservations found\n";
          } else {
            int resID = rs.getInt("resID");
            String paid = "";
            if (rs.getInt("paid") == 0) {
              paid = "false";
            } else {
              paid = "true";
            }
            int fid1 = rs.getInt("fid1");
            int fid2 = rs.getInt("fid2");
            
            String flight1 = getFlightInfo(fid1);
            sb.append("Reservation " + resID + " paid: " + paid + ":\n");
            sb.append(flight1 + "\n");

            if (fid2 != 0) {
              String flight2 = getFlightInfo(fid2);
              sb.append(flight2 + "\n");
            }
          }

          while (rs.next()) {
            int resID = rs.getInt("resID");
            String paid = "";
            if (rs.getInt("paid") == 0) {
              paid = "false";
            } else {
              paid = "true";
            }
            int fid1 = rs.getInt("fid1");
            int fid2 = rs.getInt("fid2");
            
            String flight1 = getFlightInfo(fid1);
            sb.append("Reservation " + resID + " paid: " + paid + ":\n");
            sb.append(flight1 + "\n");

            if (fid2 != 0) {
              String flight2 = getFlightInfo(fid2);
              sb.append(flight2 + "\n");
            }
          }

          rs.close();
          conn.commit();
          conn.setAutoCommit(true);
          return sb.toString();
        } catch(SQLException e) {
          deadlock = isDeadlock(e);
          if (deadlock) {
            try {
              conn.rollback();
              conn.setAutoCommit(true);
            } catch (SQLException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
      return "Failed to retrieve reservations\n";
    } finally {
      checkDanglingTransaction();
    }
  }

  // Private helper method to generate flight info given a fid
  private String getFlightInfo(int fid) {
    Flight flight;

    try {
      generateFlightInfoStmt.setInt(1, fid);
      ResultSet results = generateFlightInfoStmt.executeQuery();

      results.next();
      flight = formatFlight(results.getInt("fid"), results.getInt("day_of_month"), results.getString("carrier_id"),
                            results.getString("flight_num"), results.getString("origin_city"), results.getString("dest_city"),
                            results.getInt("actual_time"), results.getInt("capacity"), results.getInt("price"));

      results.close();
    } catch (SQLException e) {
      return "";
    }

    return flight.toString();
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }
}
