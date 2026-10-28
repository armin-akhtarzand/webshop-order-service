# Webshop Order Service

Order Service ansvarar för beställningar och Stripe Checkout
i webshoppen. Beställningar och betalningsstatus lagras i PostgreSQL.

## Ansvar och samverkan

- Skapar beställningar för den inloggade användaren.
- Anropar Product Service för att minska lagersaldot.
- Beräknar beställningens totalpris från produktinformationen.
- Hämtar en beställning via dess ID.
- Skapar Stripe Checkout-sessioner för betalning i SEK.
- Tar emot och verifierar Stripe-webhooks.
- Skickar orderbekräftelser via RabbitMQ efter bekräftad betalning.

Frontend anropar Order Service via Gateway.
Auth Service utfärdar JWT-token som Order Service verifierar
med publika nycklar från Auth Services JWKS-endpoint.

Mail Service tar emot orderbekräftelser från RabbitMQ
och skickar e-post till kunden.

## Tekniker

- Java 17
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security och OAuth2 Resource Server
- Spring Data JPA och PostgreSQL
- Spring AMQP och RabbitMQ
- Stripe Java SDK 34.0.0
- Bean Validation
- Springdoc OpenAPI 3.0.2 och Swagger UI
- Lombok
- Maven och Maven Wrapper

## Förutsättningar

- JDK 17.
- PostgreSQL med förberett databasschema.
- Auth Service och Product Service.
- Tillgång till RabbitMQ.
- Stripe-testnyckel och webhook-hemlighet för testmiljön.
- Git för att klona projektet.

För hela köpflödet behövs även Gateway och Frontend.
Mail Service behöver vara igång för att behandla orderbekräftelser.

Maven Wrapper ingår, så Maven behöver inte installeras separat.

## Hämta projektet

```bash
git clone https://github.com/armin-akhtarzand/webshop-order-service.git
cd webshop-order-service
```

## Miljövariabler

Konfigurationen finns i `src/main/resources/application.properties`.

### Obligatoriska variabler

- `DB_URL`: JDBC-adress till PostgreSQL.
  Exempel: `jdbc:postgresql://localhost:5432/webshop_order`.
- `DB_USERNAME`: databasens användarnamn.
- `DB_PASSWORD`: databasens lösenord.
- `RABBITMQ_HOST`: RabbitMQ-serverns värdnamn.
- `RABBITMQ_USERNAME`: användarnamn för RabbitMQ.
  Samma värde används även som virtual host.
- `RABBITMQ_PASSWORD`: lösenord för RabbitMQ.
- `STRIPE_SECRET_KEY`: hemlig Stripe-API-nyckel.
  Använd en testnyckel vid lokal utveckling.
- `STRIPE_WEBHOOK_SECRET`: signeringshemlighet för den webhook
  som levererar händelser till denna instans.

### Variabler med standardvärden

- `PORT`: `9999`.
- `PRODUCT_SERVICE_BASE_URL`: `http://localhost:8081`.
- `AUTH_SERVER_URL`: `http://localhost:8080`.
- `RABBITMQ_PORT`: `5671`.

JWKS-adressen byggs genom att lägga till `/auth/jwks`
efter `AUTH_SERVER_URL`. Ange därför Auth Services basadress,
utan denna sökväg och utan avslutande snedstreck.

Ange variablerna i terminalen eller i IntelliJ under
Run → Edit Configurations → Environment variables.

En `.env`-fil läses inte automatiskt av den visade konfigurationen.
Spara inte riktiga lösenord, API-nycklar eller webhook-hemligheter i Git.

## Databas

Tjänsten använder PostgreSQL för beställningar, orderrader,
totalpris, Stripe-sessionens ID och betalningsstatus.

Standardkonfigurationen är:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Hibernate skapar eller uppdaterar därför inte tabeller automatiskt.
Databasen och de tabeller som entiteterna behöver måste förberedas.

För teamets befintliga databas används det förberedda schemat.
Kontakta teamet för databasåtkomst och instruktioner för
att förbereda en ny lokal databas.

H2 finns som beroende, men standardkonfigurationen använder PostgreSQL.

## RabbitMQ och orderbekräftelser

Orderbekräftelser skickas som JSON till kön:

```text
order-confirmation-queue
```

RabbitTemplate använder standard-exchange med könamnet som routing key.
Mail Service behöver använda samma RabbitMQ-server, virtual host
och kö för att ta emot meddelandena.

Meddelandet innehåller kundens e-postadress, orderdatum,
orderrader och totalpris.

Kundens identitet från `Principal.getName()` sparas som kundnamn
och används även som mottagarens e-postadress.
Inloggningsidentiteten behöver därför vara en användbar e-postadress.

Standardkonfigurationen använder:

- SSL/TLS aktiverat.
- Port `5671`.
- `RABBITMQ_USERNAME` som både användarnamn och virtual host.

Att bara ändra porten till `5672` stänger inte av SSL.
En lokal broker utan TLS behöver även andra anslutningsinställningar.
Använd teamets broker eller konfigurera en separat lokal utvecklingsmiljö.

## Starta lokalt

1. Förbered PostgreSQL-databasen och dess schema.
2. Ange miljövariablerna.
3. Kontrollera RabbitMQ-anslutningen.
4. Starta Auth Service och Product Service.
5. Starta Order Service från projektets rotmapp.

### macOS och Linux

```bash
./mvnw spring-boot:run
```

Om skriptet saknar körbehörighet:

```bash
bash ./mvnw spring-boot:run
```

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

Tjänstens standardadress är:

```text
http://localhost:9999
```

Om porten ändras behöver Gateways adress till Order Service
och webhookens leveransadress också uppdateras.

Starta Gateway och Frontend för att använda webbgränssnittet.
Starta även Mail Service för att behandla orderbekräftelser.

## API

- `POST /orders`: skapar en beställning.
- `GET /orders/{id}`: hämtar en beställning.
  Returnerar 404 om beställningen inte finns.
- `POST /orders/checkout`: skapar en beställning och en
  Stripe Checkout-session. Returnerar `checkoutUrl` och `orderId`.
- `POST /stripe/webhook`: tar emot Stripe-händelser.

Order-endpoints kräver en giltig JWT-token.
Vid direkta anrop till Order Service skickas token i headern:

```text
Authorization: Bearer <JWT_TOKEN>
```

Swagger-adresserna och `/stripe/webhook` är undantagna
från kravet på JWT i SecurityConfig.
Webhooken verifierar i stället Stripes signatur
med headern `Stripe-Signature`.

Se DTO-klasserna och Swagger UI för request- och responseformat.

## Betalningsflöde

1. Frontend anropar `POST /orders/checkout` via Gateway.
2. Order Service anropar Product Service för att minska lagersaldot.
3. Beställningen sparas och en Stripe Checkout-session skapas.
4. Frontend öppnar den returnerade `checkoutUrl`.
5. Stripe skickar en webhook till Order Service.
6. När betalningen bekräftats ändras status från `PENDING` till `PAID`.
7. Order Service skickar en orderbekräftelse till RabbitMQ.
8. Mail Service behandlar meddelandet och skickar e-post.

`POST /orders/checkout` skapar själv en beställning.
Anropa därför inte först `POST /orders` för samma köp.

Lagersaldot minskas redan när beställningen skapas.
Den visade webhook-koden återställer inte lagret vid misslyckad
eller utgången betalning.

### Stripe-händelser

Webhooken behandlar:

- `checkout.session.completed`
- `checkout.session.async_payment_succeeded`
- `checkout.session.async_payment_failed`
- `checkout.session.expired`

För de två första händelserna krävs även att Stripes
`payment_status` är `paid` innan beställningen markeras som betald.

Misslyckad asynkron betalning eller utgången session
ändrar en väntande beställning till `FAILED`.

Händelser för en beställning som inte längre är `PENDING`
behandlas inte igen av den aktuella koden.

### Returadresser

Checkout använder för närvarande fasta returadresser:

- Lyckad Checkout:
  `http://localhost:5173/payment/success?orderId=<orderId>`
- Avbruten Checkout:
  `http://localhost:5173/payment/cancel`

Frontend behöver därför köras på port `5173` för dessa returadresser.
Adresserna finns i `CustomerOrderService.createCheckoutSession()`
och styrs inte av miljövariabler i den visade implementationen.

En omdirigering till success-sidan ändrar inte i sig betalningsstatus.
Det gör den verifierade webhook-händelsen.

### Lokal webhook

Stripe behöver kunna leverera händelser till:

```text
http://localhost:9999/stripe/webhook
```

Vid lokal utveckling behövs vidarebefordran till denna adress,
exempelvis med Stripe CLI, eftersom en extern webhook inte kan
nå datorns localhost direkt.

`STRIPE_WEBHOOK_SECRET` måste motsvara den webhook eller
vidarebefordran som faktiskt levererar händelserna.

## API-dokumentation

När tjänsten körs på standardporten finns dokumentationen här:

- Swagger UI: http://localhost:9999/swagger-ui/index.html
- OpenAPI: http://localhost:9999/v3/api-docs

Dokumentationen är tillgänglig utan inloggning.
Skyddade orderanrop kräver fortfarande giltig JWT-token.

## Test och bygge

Kör tester:

```bash
./mvnw test
```

Bygg och verifiera projektet:

```bash
./mvnw clean verify
```

På Windows används `.\mvnw.cmd` i stället för `./mvnw`.

Tester som startar applikationskontexten kan behöva
miljövariabler och beroenden enligt testkonfigurationen.

## Felsökning

- Saknad miljövariabel: kontrollera databas-, RabbitMQ-
  och Stripe-inställningarna.
- Databasfel eller saknad tabell: kontrollera anslutningen
  och att schemat har förberetts.
- Status 401: kontrollera JWT-token och Auth Services JWKS-adress.
- Produktanrop misslyckas: kontrollera Product Service,
  `PRODUCT_SERVICE_BASE_URL` och behörigheterna för anropet.
- RabbitMQ-anslutningen misslyckas: kontrollera värdnamn,
  användarnamn, lösenord, virtual host, TLS och port.
- Beställningen förblir `PENDING`: kontrollera att webhooken
  levereras, att signaturen godkänns och att betalningen är `paid`.
- Ogiltig webhook-signatur: kontrollera `STRIPE_WEBHOOK_SECRET`
  för den aktuella leveransmetoden.
- Ingen e-post: kontrollera betalningsstatus, RabbitMQ-kön,
  Mail Service och dess e-postinställningar.
- Fel returadress efter Checkout: kontrollera att Frontend
  körs på port `5173`.

## Systemets repositories

- [Auth Service](https://github.com/armin-akhtarzand/webshop-auth-server)
- [Product Service](https://github.com/armin-akhtarzand/webshop-product-service)
- [Order Service](https://github.com/armin-akhtarzand/webshop-order-service)
- [Mail Service](https://github.com/armin-akhtarzand/webshop-mail-service)
- [Gateway](https://github.com/SofiaTekle/webshop-gateway)
- [Frontend](https://github.com/SofiaTekle/webshop-frontend)