
package acme.features.customer.booking;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.booking.Booking;
import acme.entities.booking.TravelClass;
import acme.entities.flight.Flight;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingPublishService extends AbstractGuiService<Customer, Booking> {

	@Autowired
	private CustomerBookingRepository repository;


	@Override
	public void authorise() {
		boolean status = super.getRequest().getPrincipal().hasRealmOfType(Customer.class);

		if (status) {
			int customerId = super.getRequest().getPrincipal().getActiveRealm().getId();
			int bookingId = super.getRequest().getData("id", int.class);
			Booking booking = this.repository.findBookingById(bookingId);
			status = customerId == booking.getCustomer().getId();
			status = status && booking.getDraftMode();
		}

		if (status && super.getRequest().getMethod().equals("POST")) {

			Integer flightId = super.getRequest().getData("flight", Integer.class);
			Flight flight = this.repository.findFlightById(flightId);

			Collection<Flight> flights = this.repository.findAllFlights().stream().filter(f -> f.getNumberLegs() != 0).collect(Collectors.toList());
			Collection<Flight> flightsAvaiables = flights.stream().filter(f -> f.getScheduledDeparture().after(MomentHelper.getCurrentMoment()) && !f.getDraftMode()).collect(Collectors.toList());

			if (flightId != 0 && !flightsAvaiables.contains(flight))
				status = false;

			if (flight != null && flight.getDraftMode())
				status = false;

		}

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {

		int id = super.getRequest().getData("id", int.class);
		Booking booking = this.repository.findBookingById(id);

		super.getBuffer().addData(booking);
	}

	@Override
	public void bind(final Booking booking) {
		super.bindObject(booking, "flight", "locatorCode", "travelClass", "lastCardNibble");
	}

	@Override
	public void validate(final Booking booking) {
		if (booking.getLastCardNibble() == null || booking.getLastCardNibble().toString().isBlank())
			super.state(false, "lastCardNibble", "acme.validation.lastCardNibble.message");

		if (booking.getFlight() != null) {
			boolean flightPublished = booking.getFlight().getDraftMode() == false;
			super.state(flightPublished, "flight", "customer.booking.form.error.flightNotPublished");

			boolean laterFlight = MomentHelper.isAfter(booking.getFlight().getScheduledDeparture(), MomentHelper.getCurrentMoment());
			super.state(laterFlight, "flight", "customer.booking.form.error.flightBeforeBooking");
		}

		boolean emptyPassengers = !this.repository.findPassengersByBookingId(booking.getId()).isEmpty();
		super.state(emptyPassengers, "price", "customer.booking.form.error.emptyPassengers");

		boolean publishedPassengers = this.repository.findPassengersByBookingId(booking.getId()).stream().allMatch(p -> p.getDraftMode() == false);
		super.state(publishedPassengers, "price", "customer.booking.form.error.unpublishedPassengers");
	}

	@Override
	public void perform(final Booking booking) {
		booking.setPurchaseMoment(MomentHelper.getCurrentMoment());
		booking.setDraftMode(false);
		this.repository.save(booking);
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		SelectChoices travelClasses = SelectChoices.from(TravelClass.class, booking.getTravelClass());

		Collection<Flight> flights = this.repository.findAllFlights().stream().filter(f -> f.getNumberLegs() != 0).collect(Collectors.toList());
		Collection<Flight> flightsAvaiables = flights.stream().filter(f -> f.getScheduledDeparture().after(MomentHelper.getCurrentMoment()) && !f.getDraftMode()).collect(Collectors.toList());

		SelectChoices flightChoices = SelectChoices.from(flightsAvaiables, "label", booking.getFlight());

		dataset = super.unbindObject(booking, "flight", "locatorCode", "travelClass", "price", "lastCardNibble", "draftMode", "id");
		dataset.put("travelClasses", travelClasses);
		dataset.put("flights", flightChoices);
		super.getResponse().addData(dataset);
	}

}
