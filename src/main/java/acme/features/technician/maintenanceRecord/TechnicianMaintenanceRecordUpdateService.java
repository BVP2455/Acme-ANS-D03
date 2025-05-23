
package acme.features.technician.maintenanceRecord;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenance.MaintenanceRecord;
import acme.entities.maintenance.MaintenanceStatus;
import acme.realms.technician.Technician;

@GuiService
public class TechnicianMaintenanceRecordUpdateService extends AbstractGuiService<Technician, MaintenanceRecord> {

	@Autowired
	private TechnicianMaintenanceRecordRepository repository;


	@Override
	public void authorise() {
		boolean status = false;
		int id;
		MaintenanceRecord mr;
		int technicianId;

		if (super.getRequest().hasData("id", Integer.class) && super.getRequest().getMethod().equals("POST")) {
			id = super.getRequest().getData("id", int.class);
			mr = this.repository.findMaintenanceRecordById(id);
			if (mr != null) {
				technicianId = super.getRequest().getPrincipal().getActiveRealm().getId();
				if (mr.isDraftMode()) {
					status = technicianId == mr.getTechnician().getId();
					boolean aircraftValid;
					boolean maintenanceStatusValid;
					int aircraftId = super.getRequest().getData("aircraft", int.class);
					String statusInput = super.getRequest().getData("status", String.class);
					if (aircraftId == 0)
						aircraftValid = true;
					else {
						Aircraft existingAircraft = this.repository.findAircraftById(aircraftId);
						aircraftValid = existingAircraft != null;
					}

					if (statusInput.trim().equals("0"))
						maintenanceStatusValid = true;
					else {
						maintenanceStatusValid = false;
						for (MaintenanceStatus ms : MaintenanceStatus.values())
							if (ms.name().equalsIgnoreCase(statusInput)) {
								maintenanceStatusValid = true;
								break;
							}
					}
					status = status && aircraftValid && maintenanceStatusValid;
				}
			}
		}
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		MaintenanceRecord mr;
		int id;
		id = super.getRequest().getData("id", int.class);

		mr = this.repository.findMaintenanceRecordById(id);
		super.getBuffer().addData(mr);
	}

	@Override
	public void bind(final MaintenanceRecord mr) {
		super.bindObject(mr, "nextInspectionDue", "estimatedCost", "notes", "status", "aircraft");
	}

	@Override
	public void validate(final MaintenanceRecord mr) {
		boolean confirmation;

		if (mr.getEstimatedCost() != null) {
			boolean validCurrency = mr.getEstimatedCost().getCurrency().equals("EUR") || mr.getEstimatedCost().getCurrency().equals("USD") || mr.getEstimatedCost().getCurrency().equals("GBP");
			super.state(validCurrency, "estimatedCost", "acme.validation.validCurrency.message");
		}

		boolean isDraft = mr.isDraftMode();

		if (!isDraft)
			super.state(isDraft, "*", "acme.validation.maintenanceRecord.published.message");

		confirmation = super.getRequest().getData("confirmation", boolean.class);
		super.state(confirmation, "confirmation", "acme.validation.confirmation.message");
	}

	@Override
	public void perform(final MaintenanceRecord mr) {
		this.repository.save(mr);
	}

	@Override
	public void unbind(final MaintenanceRecord mr) {
		Dataset dataset;
		SelectChoices statusChoices;
		statusChoices = SelectChoices.from(MaintenanceStatus.class, mr.getStatus());
		SelectChoices aircraftChoices;
		Collection<Aircraft> aircrafts;
		aircrafts = this.repository.findAllAircrafts();
		aircraftChoices = SelectChoices.from(aircrafts, "registrationNumber", mr.getAircraft());
		dataset = super.unbindObject(mr, "maintenanceMoment", "nextInspectionDue", "estimatedCost", "notes");
		dataset.put("statuses", statusChoices);
		dataset.put("aircrafts", aircraftChoices);
		dataset.put("aircraft", aircraftChoices.getSelected().getKey());
		super.getResponse().addData(dataset);
	}

}
