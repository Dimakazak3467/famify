# Famify 🛒

**Семейный список покупок** — приложение для совместного ведения списка покупок с родными или друзьями в реальном времени.

[![Firebase](https://img.shields.io/badge/Firebase-039BE5?logo=firebase)](https://firebase.google.com/)
[![Material Design](https://img.shields.io/badge/Material%20Design-3-757575?logo=material-design)](https://m3.material.io/)

## 📌 Возможности
- 🔐 Авторизация через Google
- 👨‍👩‍👧‍👦 Создание/присоединение к семейным группам
- 🛍️ Добавление, редактирование и удаление товаров
- ✅ Отметка купленных товаров
- 📱 Адаптивный Material Design 3 интерфейс
- 🔄 Синхронизация в реальном времени

## 🛠 Технологии
- **Язык**: Java
- **Backend**: Firebase (Auth, Firestore)
- **UI**: Material Components, RecyclerView, Fragments
- **Анимации**: Property Animation, Shared Elements

## 🗃 Структура базы данных
```plaintext
firestore/
├── users/{userID}
│   ├── name: string
│   ├── email: string
│   └── familyId: string
│
├── families/{familyCode}
│   ├── code: string
│   ├── createdBy: string
│   ├── members: array
│   └── products/{productID}
│       ├── name: string
│       ├── comment: string
│       ├── addedBy: string
│       ├── inCart: boolean
│       └── createdAt: timestamp
