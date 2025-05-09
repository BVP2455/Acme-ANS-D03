
package acme.entities.service;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;

@Repository
public interface ServiceRepository extends AbstractRepository {

	@Query("SELECT s FROM Service s WHERE s.promotionCode = :promotionCode")
	Service findServiceByPromotionCode(@Param("promotionCode") String promotionCode);
}
