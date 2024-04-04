package ru.dogobot.Dogobot.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

//import org.springframework.data.repository.CrudRepository;
//
//public interface UserRepository extends CrudRepository<User, Long> {
//}
