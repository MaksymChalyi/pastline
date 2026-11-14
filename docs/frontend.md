# Клієнт (React)

Односторінковий застосунок (SPA): React 19 + TypeScript, збірка Vite 8, маршрути react-router 7, серверні дані
через TanStack Query 5, власний CSS без UI-бібліотек. Адаптивний: на телефоні навігація — нижня панель вкладок.

## Структура

```
frontend/
├── index.html, public/favicon.svg
├── vite.config.ts        # dev-сервер :5173, проксі /api → 127.0.0.1:8090
├── vercel.json           # rewrite /api/* на Render і SPA-fallback на index.html
├── eslint.config.js, .prettierrc
└── src/
    ├── main.tsx          # QueryClient (401 → вихід, повтори лише для 5xx), BrowserRouter
    ├── App.tsx           # маршрути
    ├── styles.css        # змінні кольорів і шрифтів, усі стилі
    ├── api/              # HTTP: client.ts + по файлу на модуль бекенду, types.ts — DTO
    ├── auth/useAuth.ts   # useMe, useLogin, useRegister, useLogout
    ├── entries/useEntries.ts
    ├── layout/           # AppLayout (захищений макет), PublicOnly, Logo, Icons
    ├── lib/dates.ts      # дати без зсуву часового поясу
    └── pages/            # по сторінці на маршрут
```

## Маршрути

| Шлях | Сторінка | Доступ |
|---|---|---|
| `/login`, `/register` | `LoginPage`, `RegisterPage` (спільна `AuthForm`) | лише без сесії (`PublicOnly`) |
| `/` | `EntriesPage` — стрічка з фільтром дат | сесія (`AppLayout`) |
| `/entries/new`, `/entries/:id` | `EntryEditorPage` | сесія |
| `/search` | `SearchPage` | сесія |
| `/ask` | `AskPage` | сесія |
| `/import` | `ImportPage` | сесія |
| інше | редирект на `/` | |

## Ключові рішення

### Один HTTP-клієнт (`api/client.ts`)

Аналог `RestClient` з interceptor'ом у Spring. Функція `api<T>(path, {method, body})`:

- додає префікс `/api/v1` і `credentials: 'include'` (cookie сесії);
- для змінних запитів читає cookie `XSRF-TOKEN` і ставить заголовок `X-XSRF-TOKEN` (немає cookie → спершу
  `GET /auth/csrf`);
- на `403 CSRF_INVALID` бере свіжий токен і повторює запит **один** раз (після входу токен змінюється);
- помилку бекенду перетворює на `ApiError {status, code, message, fieldErrors}` — той самий формат, що
  `ErrorResponse` на бекенді;
- для `FormData` не ставить `Content-Type`: межу multipart браузер додає сам.

`api/types.ts` — копії Java-record'ів DTO. TypeScript перевіряє їх лише під час компіляції, тож після зміни DTO
типи треба оновити вручну.

### Серверний стан — TanStack Query

Дані з бекенду не кладуться в `useState`. `useQuery` кешує відповідь за ключем, показує стан завантаження й
помилки, повторює запит при збої мережі; `useMutation` змінює дані й скидає кеш.

| Ключ | Що | Коли скидається |
|---|---|---|
| `['me']` | поточний користувач | вхід/вихід, будь-який 401 |
| `['entries', 'list', filter]` | сторінка стрічки | після створення, зміни, видалення, імпорту |
| `['entries', 'detail', id]` | один запис | після його зміни чи видалення |

Усі ключі записів починаються з `'entries'`: `invalidateQueries({queryKey: ['entries']})` скидає все одним
викликом (аналог `@CacheEvict(allEntries = true)`).

Налаштування `QueryClient` (`main.tsx`): повтор лише для мережевих збоїв і 5xx (4xx від повтору не зміниться),
без перезапиту при фокусі вікна, а будь-який 401 ставить `['me'] = null` — і `AppLayout` переводить на `/login`.

### Стан в адресі

Фільтр дат, номер сторінки, пошуковий запит і режим живуть у query-параметрах URL (`useSearchParams`), а не в
`useState`. Тому оновлення сторінки, кнопка «назад» і посилання на конкретний пошук працюють без додаткового коду.

### Форми

Контрольовані поля (`value` + `onChange` → `useState`). Помилки полів з `ApiError.fieldErrors` показуються списком
у формі. Редактор запису отримує `key` за id, тож для іншого запису форма створюється заново з
початковими значеннями, без `useEffect` для «синхронізації» стану.

### Дати

`entryDate` приходить рядком `"2026-03-14"`. `new Date("2026-03-14")` трактує його як UTC-північ і в поясах з
мінусом показує попередній день, тому `lib/dates.ts` розбирає рядок на рік/місяць/день вручну. «Сьогодні» для
форми береться в часовому поясі браузера.

### Стилі

Один `styles.css`: змінні в `:root` (`--bg`, `--surface`, `--text`, `--accent`, `--font-prose` …), без
CSS-фреймворку. Брейкпоінти `1023px` і `767px`; на телефоні — нижня панель вкладок, поля 16 px (iOS не збільшує
масштаб), зони дотику ≥ 44 px. Іконки — inline SVG з `currentColor`.

## Команди

```powershell
cd frontend
npm install
npm run dev              # http://localhost:5173
npm run dev -- --host    # доступ з телефона в тій самій Wi-Fi
npm run format           # Prettier: переформатувати все
npm run lint             # ESLint
npm run check            # format:check + lint + build (з tsc) — перед комітом
npm run build            # dist/
npm run preview          # перегляд зібраного dist/
```

## Якість коду

- **Prettier** (`.prettierrc`): без крапок з комою, одинарні лапки, ширина 110, коми в кінці.
- **ESLint** (`eslint.config.js`): `@eslint/js` + `typescript-eslint` (recommended), правила хуків React
  (`eslint-plugin-react-hooks`), `react-refresh`, `eslint-config-prettier` вимикає правила, що конфліктують з
  Prettier. Невикористані змінні з префіксом `_` дозволені.
- `tsconfig.json` — `strict`.

## Для Java-розробника

| React / TS | Аналог у Spring / Java |
|---|---|
| компонент-функція | метод, що повертає шаблон; викликається при кожній зміні стану |
| `useState` | поле об'єкта, зміна якого перемальовує компонент |
| `useEffect(fn, [])` + функція очищення | `@PostConstruct` + `@PreDestroy` |
| `useQuery` | `@Cacheable` метод з автоматичним станом loading / error |
| `invalidateQueries` | `@CacheEvict` |
| `api/client.ts` | `RestClient` з interceptor'ом |
| `api/types.ts` | DTO-record'и (без перевірки під час виконання) |
| `<Routes>` | `@RequestMapping`, але сторінку обирає браузер |
| `Outlet` у макеті | layout-шаблон, у який підставляється дочірня сторінка |

Детальніше — [learning/concepts.md](learning/concepts.md#react-для-java-розробника).
