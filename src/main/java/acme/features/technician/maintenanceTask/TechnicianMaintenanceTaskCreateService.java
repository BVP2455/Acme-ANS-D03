
package acme.features.technician.maintenanceTask;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.controllers.GuiController;
import acme.client.services.AbstractGuiService;
import acme.entities.maintenance.MaintenanceRecord;
import acme.entities.maintenance.MaintenanceTask;
import acme.entities.maintenance.Task;
import acme.realms.technician.Technician;

@GuiController
public class TechnicianMaintenanceTaskCreateService extends AbstractGuiService<Technician, MaintenanceTask> {

	@Autowired
	private TechnicianMaintenanceTaskRepository repository;


	@Override
	public void authorise() {
		boolean status = false;

		int technicianId = super.getRequest().getPrincipal().getActiveRealm().getId();
		if (super.getRequest().getMethod().equals("GET"))
			status = !super.getRequest().hasData("id", Integer.class);
		else if (super.getRequest().getMethod().equals("POST")) {
			int mrId = super.getRequest().getData("mrId", int.class);
			MaintenanceRecord mr = this.repository.findMaintenanceRecordById(mrId);
			if (mr != null && mr.getTechnician().getId() == technicianId && mr.isDraftMode()) {
				status = true;

				int id = super.getRequest().getData("id", int.class);
				boolean validId = id == 0;

				boolean taskValid = true;
				int taskId = super.getRequest().getData("task", int.class);
				if (taskId == 0)
					taskValid = true;
				else {
					Task existingTask = this.repository.findTaskById(taskId);
					Collection<Task> tasksOfMaintenanceRecord = this.repository.findTasksByMaintenanceRecord(mrId);
					boolean alreadySelected = tasksOfMaintenanceRecord.contains(existingTask);
					taskValid = existingTask != null && !alreadySelected && (!existingTask.isDraftMode() || existingTask.getTechnician().equals(mr.getTechnician()));
				}

				status = status && taskValid && validId;
			}

		}
		super.getResponse().setAuthorised(status);

	}

	@Override
	public void load() {
		int mrId;
		MaintenanceRecord mr;
		MaintenanceTask maintenanceTask;

		mrId = super.getRequest().getData("mrId", int.class);
		mr = this.repository.findMaintenanceRecordById(mrId);

		maintenanceTask = new MaintenanceTask();
		maintenanceTask.setMaintenanceRecord(mr);

		super.getBuffer().addData(maintenanceTask);
	}

	@Override
	public void bind(final MaintenanceTask maintenanceTask) {
		int mrId;
		MaintenanceRecord mr;

		mrId = super.getRequest().getData("mrId", int.class);
		mr = this.repository.findMaintenanceRecordById(mrId);

		super.bindObject(maintenanceTask, "task");
		maintenanceTask.setMaintenanceRecord(mr);
	}

	@Override
	public void validate(final MaintenanceTask mt) {
		;
	}

	@Override
	public void perform(final MaintenanceTask mt) {
		this.repository.save(mt);
	}

	@Override
	public void unbind(final MaintenanceTask mt) {
		int mrId;
		Collection<Task> availableTasks;
		SelectChoices choices;
		Dataset dataset;

		int technicianId = super.getRequest().getPrincipal().getActiveRealm().getId();

		mrId = mt.getMaintenanceRecord().getId();
		availableTasks = this.repository.findAllAvailableTasks(mrId, technicianId);
		choices = SelectChoices.from(availableTasks, "description", mt.getTask());

		dataset = super.unbindObject(mt);
		dataset.put("mrId", super.getRequest().getData("mrId", int.class));
		dataset.put("task", choices.getSelected().getKey());
		dataset.put("tasks", choices);

		super.getResponse().addData(dataset);
	}

}
