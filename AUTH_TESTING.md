# Тестирование регистрации и авторизации

## Запуск приложения

```bash
docker-compose up -d --build
```

## Создание тестовых пользователей

### Создание администратора
```bash
docker exec education_backend wget -q -O- --post-data='{"username":"admin","password":"admin123","email":"admin@example.com"}' --header='Content-Type: application/json' http://localhost:8080/api/auth/register
```

### Назначение роли ADMIN
```bash
docker exec education_db psql -U postgres -d education_platform -c "UPDATE users SET role = 'ADMIN' WHERE username = 'admin';"
```

### Создание обычного пользователя
```bash
docker exec education_backend wget -q -O- --post-data='{"username":"testuser","password":"user123","email":"user@example.com"}' --header='Content-Type: application/json' http://localhost:8080/api/auth/register
```

## Тестирование через web-интерфейс

1. Откройте браузер и перейдите по адресу: http://localhost:8081
2. Нажмите на "Регистрация" в навигации
3. Заполните форму регистрации:
   - Имя пользователя: `newuser`
   - Email: `newuser@example.com`
   - Пароль: `password123`
4. Нажмите "Зарегистрироваться"
5. После успешной регистрации перейдите на страницу "Вход"
6. Введите учетные данные и нажмите "Войти"

## Тестирование через API

### Регистрация
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123","email":"newuser@example.com"}'
```

Ожидаемый ответ:
```json
{"success":true,"message":"User registered successfully","username":"newuser"}
```

### Авторизация
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"password123"}'
```

Ожидаемый ответ:
```json
{"role":"USER","success":true,"message":"Login successful","username":"newuser"}
```

### Попытка авторизации с неправильным паролем
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"wrongpassword"}'
```

Ожидаемый ответ (HTTP 401):
```json
{"success":false,"message":"Invalid username or password"}
```

## Автоматический тест

Запустите скрипт для автоматического тестирования:
```bash
./test_auth.sh
```

## Проверка данных в базе

```bash
# Просмотр всех пользователей
docker exec education_db psql -U postgres -d education_platform -c "SELECT id, username, email, role FROM users;"

# Удаление пользователя
docker exec education_db psql -U postgres -d education_platform -c "DELETE FROM users WHERE username = 'testuser';"
```

## Учетные данные тестовых пользователей

После первоначальной настройки доступны следующие пользователи:

| Логин | Пароль | Роль |
|-------|--------|------|
| admin | admin123 | ADMIN |
| testuser | user123 | USER |

## Реализованные функции

✅ Регистрация пользователей с проверкой уникальности username и email  
✅ Хеширование паролей через BCrypt  
✅ Авторизация пользователей с проверкой учетных данных  
✅ Возврат роли пользователя при успешной авторизации  
✅ Обработка ошибок (неверные учетные данные, дубликаты)  
✅ Web-интерфейс для регистрации и авторизации  
✅ REST API для интеграции с frontend  
✅ CORS настройки для работы между сервисами  
