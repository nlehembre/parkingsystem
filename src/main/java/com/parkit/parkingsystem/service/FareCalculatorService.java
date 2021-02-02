package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    private TicketDAO ticketDAO;

    public void setTicketDAO(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }
    public void calculateFare(Ticket ticket){
        boolean recurrentUser = ticketDAO != null && ticketDAO.isRecurrentUser(ticket.getVehicleRegNumber());
        calculateFare(ticket, recurrentUser);
    }

    private void calculateFare(Ticket ticket, boolean recurrentUser) {
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        long durationMn = (outHour - inHour) / (60 * 1000) ;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(durationMn * Fare.CAR_RATE_PER_HOUR / 60);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationMn * Fare.BIKE_RATE_PER_HOUR / 60 );
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        if (durationMn < 30) {
            ticket.setPrice(0);
        }
        if (recurrentUser) {
            ticket.setPrice(0.95 * ticket.getPrice());
        }
    }
}