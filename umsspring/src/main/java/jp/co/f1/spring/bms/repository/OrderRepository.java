package jp.co.f1.spring.bms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.f1.spring.bms.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

	public Iterable<Order> findByUserid(String userid);
}
