
package acme.features.flightcrewmember.activitylog;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.activitylog.ActivityLog;
import acme.entities.flightassignment.FlightAssignment;

@Repository
public interface ActivityLogRepository extends AbstractRepository {

	@Query("SELECT al FROM ActivityLog al")
	Collection<ActivityLog> findAllActivityLogs();

	@Query("SELECT al FROM ActivityLog al WHERE al.activityLogAssignment.id = :flightAssignmentId")
	Collection<ActivityLog> findAllActivityLogsByFlightAssignmentId(int flightAssignmentId);

	@Query("SELECT fa FROM FlightAssignment fa WHERE fa.id = :flightAssignmentId")
	FlightAssignment findFlightAssignmentById(int flightAssignmentId);

	@Query("SELECT al FROM ActivityLog al WHERE al.id = :id")
	ActivityLog findActivityLogById(int id);

	@Query("SELECT fa FROM FlightAssignment fa")
	Collection<FlightAssignment> findAllFlightAssignments();

}
