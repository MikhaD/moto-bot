build:
	docker-compose -f ./development/docker-compose.yml up -d --build

up:
	docker-compose -f ./development/docker-compose.yml up -d

down:
	docker-compose -f ./development/docker-compose.yml down

test:
	docker-compose -f ./development/docker-compose.yml exec moto-bot mvn test

db-up:
	docker-compose -f ./development/docker-compose.yml up -d mysql

db:
	docker-compose -f ./development/docker-compose.yml exec mysql mysql -u root -p

javadoc: # Downloads javadoc
	mvn dependency:resolve -Dclassifier=javadoc
