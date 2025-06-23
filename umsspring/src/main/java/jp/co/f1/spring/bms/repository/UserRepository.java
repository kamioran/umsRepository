package jp.co.f1.spring.bms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.f1.spring.bms.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

	public Optional<User> findByUserid(String userid);

	public Optional<User> findByUseridAndPassword(String userid, String password);
}
