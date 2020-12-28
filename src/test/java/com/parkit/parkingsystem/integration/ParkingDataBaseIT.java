package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACarTicketSaved() throws Exception {
        //TODO: check that a ticket is actualy saved in DB (and Parking table is updated with availability)

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        //THEN
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = dataBaseTestConfig.getConnection();
            ps = con.prepareStatement("select PARKING_NUMBER from ticket where VEHICLE_REG_NUMBER=? ;");
            ps.setString(1,inputReaderUtil.readVehicleRegistrationNumber());
            rs = ps.executeQuery();
            assertTrue(rs.next());
        } finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingACarAvailabilityUpdated() throws Exception {
        //TODO: check that (a ticket is actualy saved in DB and) Parking table is updated with availability

        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        //THEN
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = dataBaseTestConfig.getConnection();
            ps = con.prepareStatement("select AVAILABLE from parking where PARKING_NUMBER=? ;");
            ps.setInt(1,inputReaderUtil.readSelection());
            rs = ps.executeQuery();
            rs.next();
            assertEquals(false, rs.getBoolean(1));
        }finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
    }

    @Test
    public void testParkingLotExit() throws Exception {
        //TODO: check that the fare generated and out time are populated correctly in the database
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        try {
            con = dataBaseTestConfig.getConnection();
            ps = con.prepareStatement("update ticket set IN_TIME=SUBTIME(IN_TIME,  '0 1:1:1.000000') where VEHICLE_REG_NUMBER=? ;");
            ps.setString(1,inputReaderUtil.readVehicleRegistrationNumber());
            ps.execute();
        }finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
        // WHEN
        parkingService.processExitingVehicle();

        //THEN

        try {
            con = dataBaseTestConfig.getConnection();
            ps = con.prepareStatement("select PRICE, OUT_TIME from ticket where VEHICLE_REG_NUMBER=? ;");
            ps.setString(1,inputReaderUtil.readVehicleRegistrationNumber());
            rs = ps.executeQuery();
            rs.next();
            double fare = rs.getDouble(1);
            Timestamp outTime = rs.getTimestamp(2);
            assertNotNull(outTime);
            double price0 = 0;
            assertNotEquals(price0, fare);
         }finally {
            dataBaseTestConfig.closeResultSet(rs);
            dataBaseTestConfig.closePreparedStatement(ps);
            dataBaseTestConfig.closeConnection(con);
        }
    }
}
