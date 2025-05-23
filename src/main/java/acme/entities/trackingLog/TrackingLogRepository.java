
package acme.entities.trackingLog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;

@Repository
public interface TrackingLogRepository extends AbstractRepository {

	@Query("SELECT t FROM TrackingLog t WHERE t.claim.id = :claimId ORDER BY t.registrationMoment DESC")
	List<TrackingLog> findTrackingLogsByClaimId(@Param("claimId") int claimId);

	@Query("SELECT t FROM TrackingLog t WHERE t.claim.leg.id = :legId")
	Collection<TrackingLog> findTrackingLogsByLegId(int legId);

}
