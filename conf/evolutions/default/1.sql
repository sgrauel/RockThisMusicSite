
# --- !Ups

CREATE TABLE Contact (
       id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
       name VARCHAR(71) NOT NULL,
       address VARCHAR(95) NOT NULL,
       city VARCHAR(35) NOT NULL,
       state VARCHAR(32) NOT NULL,
       zip INT NOT NULL,
       country VARCHAR(2) NOT NULL,
       phone VARCHAR(10) NOT NULL,
       email VARCHAR(254) NOT NULL,
       message MEDIUMTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# --- !Downs

-- DROP TABLE Contact;


