#!/bin/bash

echo "1. Регистрация нового пользователя (testuser3)..."
REGISTER_RESPONSE=$(docker exec education_backend wget -q -O- --post-data='{"username":"testuser3","password":"test123","email":"testuser3@example.com"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/api/auth/register | jq)
echo "Ответ: $REGISTER_RESPONSE"

# Тест 2: Попытка зарегистрировать пользователя с тем же именем (должна быть ошибка)
echo "2. Попытка повторной регистрации (должна быть ошибка)..."
REGISTER_DUP=$(docker exec education_backend wget -q -O- --post-data='{"username":"testuser3","password":"test123","email":"another@example.com"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/api/auth/register)
echo "Ответ: $REGISTER_DUP"
echo ""

# Тест 3: Авторизация с правильным паролем
echo "3. Авторизация с правильным паролем..."
LOGIN_SUCCESS=$(docker exec education_backend wget -q -O- --post-data='{"username":"testuser3","password":"test123"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/api/auth/login)
echo "Ответ: $LOGIN_SUCCESS"
echo ""

# Тест 4: Авторизация с неправильным паролем
echo "4. Авторизация с неправильным паролем (должна быть ошибка)..."
LOGIN_FAIL=$(docker exec education_backend wget -q -O- --post-data='{"username":"testuser3","password":"wrongpassword"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/api/auth/login 2>&1 || true)
echo "Ответ: $LOGIN_FAIL"
echo ""

# Тест 5: Проверка существующего пользователя
echo "5. Авторизация существующего пользователя (admin)..."
ADMIN_LOGIN=$(docker exec education_backend wget -q -O- --post-data='{"username":"admin","password":"admin123"}' \
  --header='Content-Type: application/json' \
  http://localhost:8080/api/auth/login 2>&1 || true)
echo "Ответ: $ADMIN_LOGIN"
echo ""

echo "=== Тестирование завершено ==="
