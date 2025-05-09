
package acme.features.technician.maintenanceRecord;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenance.MaintenanceRecord;
import acme.entities.maintenance.MaintenanceStatus;
import acme.entities.maintenance.Task;
import acme.realms.technician.Technician;

@GuiService
public class TechnicianMaintenanceRecordPublishService extends AbstractGuiService<Technician, MaintenanceRecord> {

	@Autowired
	private TechnicianMaintenanceRecordRepository repository;


	@Override
	public void authorise() {
		boolean status = false;
		int id;
		MaintenanceRecord mr;
		Technician technician;
		id = super.getRequest().getData("id", int.class);
		mr = this.repository.findMaintenanceRecordById(id);
		technician = mr == null ? null : mr.getTechnician();
		if (super.getRequest().getPrincipal().hasRealm(technician) && mr != null && mr.isDraftMode()) {
			status = true;

			if (super.getRequest().getMethod().equals("POST")) {
				Aircraft aircraft = super.getRequest().getData("aircraft", Aircraft.class);
				Aircraft existingAircraft = aircraft != null ? this.repository.findAircraftById(aircraft.getId()) : null;
				boolean aircraftValid = existingAircraft != null && aircraft.getId() != 0;

				String statusInput = super.getRequest().getData("status", String.class);
				boolean statusValid = false;
				if (statusInput.trim().equals("0"))
					statusValid = true;
				else {
					statusValid = false;
					for (MaintenanceStatus ms : MaintenanceStatus.values())
						if (ms.name().equalsIgnoreCase(statusInput)) {
							statusValid = true;
							break;
						}
				}
				status = status && statusValid && aircraftValid;
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
	}

	@Override
	public void validate(final MaintenanceRecord mr) {

		Collection<Task> tasksOfMr = this.repository.findTasksInMaintenanceRecord(mr.getId());

		if (mr.getEstimatedCost() != null) {
			boolean validCurrency = mr.getEstimatedCost().getCurrency().equals("EUR") || mr.getEstimatedCost().getCurrency().equals("USD") || mr.getEstimatedCost().getCurrency().equals("GBP");
			super.state(validCurrency, "estimatedCost", "acme.validation.validCurrency.message");
		}

		boolean isDraft = mr.isDraftMode();

		if (!isDraft)
			super.state(isDraft, "*", "acme.validation.maintenanceRecord.published.message");

		boolean valid = !tasksOfMr.isEmpty() && tasksOfMr.stream().allMatch(task -> !task.isDraftMode());

		if (!valid)
			super.state(valid, "*", "acme.validation.maintenanceRecord.incorrect-maintenanceTasks.message");

	}

	@Override
	public void perform(final MaintenanceRecord maintenanceRecord) {
		maintenanceRecord.setDraftMode(false);
		this.repository.save(maintenanceRecord);
	}

	@Override
	public void unbind(final MaintenanceRecord mr) {

	}

}
