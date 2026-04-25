# Smart Grocery - Подробен проектен обзор

Този документ е за новодошли в проекта. Целта е бързо да даде контекст какво е приложението, какво вече е реализирано, как е структурирано и в каква посока планираме да го развиваме.

---

## 1) Какво представлява проектът

`Smart Grocery` е уеб приложение за управление на домашни хранителни наличности и покупки.

Идеята е потребителите в едно домакинство да могат:
- да виждат какво имат в pantry (домашен склад),
- да поддържат количествата актуални,
- да следят критични нива (min threshold),
- и постепенно да преминат към автоматизирани предложения за пазаруване.

Накратко: проектът решава ежедневния проблем "Какво имаме вкъщи, какво свършва и какво да купим?".

---

## 2) Какво е реализирано към момента

### Аутентикация и достъп
- Регистрация и вход през уеб интерфейс (Thymeleaf):
  - `/register`
  - `/login`
- Session-based login за web flow.
- Допълнително има и API auth endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Security е разделен на web и api chain, за да не се смесват двата начина на достъп.

### Домейн и данни
- Основни модели:
  - `User`
  - `Household`
  - `HouseholdMember`
  - `Product`
  - `PantryItem`
- При регистрация се създава:
  - default household за новия user
  - owner membership в household-а
- Добавени са seed данни за продукти (Flyway migration `V2__seed_products.sql`).

### Pantry функционалност (първа версия)
- Страница `/pantry` с:
  - списък на pantry items за текущото household
  - форма за добавяне на нов item
- Валидации:
  - backend check продукт <-> мерна единица
  - UI ограничение: `Unit` се ограничава според избрания `Product`.

### Dashboard и UI
- `/dashboard` показва:
  - текущ потребител
  - household
  - брой потребители/продукти/pantry items
- Има единен стил (общ `styles.css`) и базов дизайн за:
  - login/register/dashboard/pantry/error страници.

### Error handling
- API: унифициран JSON формат за грешки.
- Web: централизирано handling на грешки + 404 template.
- Има подобрения за edge cases (напр. stale session след reset на DB).

---

## 3) Текуща архитектура (накратко)

Проектът следва layered архитектура:
- `Controller -> Service -> Repository`

Правило, което спазваме:
- Service може да вика:
  - своето repository,
  - друг service.
- Service не трябва да вика директно чуждо repository.

Това държи бизнес логиката подредена и улеснява бъдещи промени/тестове.

---

## 4) Технологии

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Thymeleaf

---

## 5) Как да пуснеш проекта локално

### Изисквания
- Java 21 (и настроен `JAVA_HOME`)
- Docker (или локален PostgreSQL)

### С Docker (препоръчително)
В root на проекта:

```powershell
docker compose up -d
.\mvnw spring-boot:run
```

След това отвори:
- `http://localhost:8080/`

### Полезни Docker команди
- Спиране на контейнерите:
```powershell
docker compose down
```
- Пълно изчистване на контейнери + volumes (трие DB данните):
```powershell
docker compose down -v
```

---

## 6) Какво е важно да знае нов човек

- Проектът е в активна фаза, не е финален продукт.
- Има работещ core flow:
  - register -> login -> dashboard -> pantry add/list
- След reset на DB:
  - може да има стари browser session cookies,
  - но логиката вече се опитва да се self-heal и да не блокира потребителя.
- Ако нещо по UI "не се вижда":
  - рестарт на app,
  - hard refresh (`Ctrl+F5`) обикновено решават проблема в dev режим.

---

## 7) Какво планираме да правим следващо

Приоритетен roadmap:

1. Pantry доработки
- Edit/Delete на pantry items
- Low-stock визуализация (`qty <= min threshold`)

2. Shopping flow
- Генериране на предложения за shopping list от pantry shortages
- Управление на shopping list items

3. Household UX
- По-ясно управление на household/member роли
- По-богат household-specific dashboard

4. Качество и стабилност
- Integration тестове за auth + основни бизнес сценарии
- Подобрени валидации и guardrails
- По-добра observability и CI стъпки

---

## 8) Предложение за работен процес

За всяка нова функционалност:
1. Малка вертикална стъпка (UI + service + persistence)
2. Ясна бизнес валидация
3. Поне базов integration test (когато тестовата среда е готова)
4. Кратка документация в `README` и/или този файл

Така поддържаме проекта лесен за разбиране и надграждане.

