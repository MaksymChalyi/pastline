import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Синтетичний корпус щоденникових записів для експериментів (EXP-001+).
 *
 * <p>Запуск з кореня репозиторію: {@code java scripts/generate-corpus/GenerateCorpus.java}. Фіксований seed: повторний
 * запуск дає байт-у-байт той самий corpus.json, що підтверджує corpus.sha256.
 *
 * <p>Кожен запис має мітку topic: це еталон релевантності для метрик Hit@k і MRR. Поля id, topic і lang імпорт
 * ігнорує.
 */
public class GenerateCorpus {

    static final long SEED = 42L;
    static final int SIZE = 520;
    static final double UA_SHARE = 0.7;
    static final LocalDate START = LocalDate.of(2024, 11, 1);
    static final int DAYS = 730;
    static final Path OUT_DIR = Path.of("docs", "experiments", "corpus-v1");

    record Pool(List<String> titles, List<String> sentences) {}

    record Entry(String id, String topic, String lang, LocalDate date, String title, String content) {}

    // Синоніми навмисно різняться між реченнями однієї теми ("пробіжка" / "біг" / "кардіо"):
    // саме на таких перефразуваннях лексичний пошук програє семантичному.
    static final Map<String, Map<String, Pool>> TOPICS = Map.of(
            "work", Map.of(
                    "uk", new Pool(
                            List.of("Робочий день", "Дедлайн", "Нарада", "Реліз", "Code review"),
                            List.of(
                                    "Зранку був стендап, обговорили блокери по задачі з платежами.",
                                    "Тімлід попросив до п'ятниці закрити інтеграцію з банківським API.",
                                    "Пів дня шукав, чому падає імпорт транзакцій на продакшені.",
                                    "Нарешті змерджили мій пул-реквест після трьох раундів рев'ю.",
                                    "Нарада з замовником затягнулась на дві години, вимоги знову змінились.",
                                    "Відчуваю, що вигораю: забагато мітингів і мало часу на код.",
                                    "Написав міграцію бази даних і покрив її тестами.",
                                    "Реліз пройшов спокійно, жодного інциденту за вечір.",
                                    "Колега пояснив, як у нас влаштований процес деплою.",
                                    "Отримав позитивний фідбек від менеджера за останній спринт.")),
                    "en", new Pool(
                            List.of("Work day", "Deadline", "Sprint review", "Release"),
                            List.of(
                                    "Spent the morning debugging a flaky integration test.",
                                    "My manager asked me to lead the next sprint planning.",
                                    "The production deploy went smoothly this evening.",
                                    "Too many meetings today, barely wrote any code.",
                                    "Finally merged the payment service refactoring.",
                                    "Had a tough conversation with the client about the scope."))),
            "finance", Map.of(
                    "uk", new Pool(
                            List.of("Гроші", "Бюджет", "Інвестиції", "Витрати місяця"),
                            List.of(
                                    "Відклав десять відсотків зарплати на резервний фонд.",
                                    "Купив трохи ОВДП, дохідність поки виглядає непогано.",
                                    "Порахував витрати за місяць: найбільше пішло на їжу та таксі.",
                                    "Поповнив брокерський рахунок і докупив ETF на індекс.",
                                    "Гривня знову просіла, добре що частина заощаджень у доларах.",
                                    "Скасував дві підписки, якими не користуюсь.",
                                    "Повернув борг другові, нарешті легше на душі.",
                                    "Склав бюджет на наступний місяць у таблиці.",
                                    "Думаю, чи варто брати розстрочку на новий ноутбук.",
                                    "Отримав премію і одразу розподілив її між цілями.")),
                    "en", new Pool(
                            List.of("Money", "Budget", "Investing"),
                            List.of(
                                    "Moved part of my salary into the emergency fund.",
                                    "Bought a few more shares of a broad market ETF.",
                                    "Tracked my spending and groceries took the biggest share.",
                                    "Cancelled a streaming subscription I never use.",
                                    "The exchange rate jumped again, glad I keep savings in dollars.",
                                    "Set up an automatic monthly transfer to my brokerage account."))),
            "health", Map.of(
                    "uk", new Pool(
                            List.of("Самопочуття", "Лікар", "Сон", "Здоров'я"),
                            List.of(
                                    "Прокинувся з головним болем, мабуть через погоду.",
                                    "Записався до терапевта, бо кашель не проходить вже тиждень.",
                                    "Спав лише п'ять годин, весь день як у тумані.",
                                    "Здав аналіз крові, чекаю результати.",
                                    "Стоматолог сказав, що треба лікувати ще один зуб.",
                                    "Почав пити більше води і менше кави.",
                                    "Вечорами болить спина від сидіння за комп'ютером.",
                                    "Застуда нарешті відступила, температури вже нема.",
                                    "Ліг спати до одинадцятої і прокинувся бадьорим.",
                                    "Лікар порадив менше екранів перед сном.")),
                    "en", new Pool(
                            List.of("Health", "Doctor", "Sleep"),
                            List.of(
                                    "Woke up with a sore throat and stayed home.",
                                    "Booked a check-up with my doctor for next week.",
                                    "Slept badly again, kept waking up at night.",
                                    "My back hurts after a long day at the desk.",
                                    "Got my blood test results, everything looks normal.",
                                    "Trying to drink less coffee in the afternoon."))),
            "family", Map.of(
                    "uk", new Pool(
                            List.of("Родина", "Батьки", "Вихідні вдома", "Свято"),
                            List.of(
                                    "Подзвонив мамі, довго говорили про дідусеве здоров'я.",
                                    "Поїхали до батьків на вихідні, тато показував новий сад.",
                                    "Сестра розповіла, що вступила до магістратури.",
                                    "Відсвяткували бабусин день народження всією родиною.",
                                    "Посварився з братом через дрібницю, треба помиритись.",
                                    "Допомагав батькам з ремонтом на кухні.",
                                    "Ввечері дивились старі сімейні фото й сміялись.",
                                    "Племінник зробив перші кроки, всі в захваті.",
                                    "Мама передала банку варення і пиріжки.",
                                    "Обговорили з батьками плани на Новий рік.")),
                    "en", new Pool(
                            List.of("Family", "Parents", "Holiday"),
                            List.of(
                                    "Called my mom and we talked for over an hour.",
                                    "Visited my parents for the weekend.",
                                    "My sister got accepted into a master's program.",
                                    "Celebrated grandma's birthday with the whole family.",
                                    "Helped my dad fix the old garden fence.",
                                    "Looked through old family photos in the evening."))),
            "travel", Map.of(
                    "uk", new Pool(
                            List.of("Подорож", "Дорога", "Нове місто", "Поїздка"),
                            List.of(
                                    "Взяв квитки на нічний потяг до Львова.",
                                    "Гуляли старим містом, зайшли в маленьку кав'ярню.",
                                    "Поїхали в Карпати, піднялись на Говерлу.",
                                    "Потяг запізнився на три години, але дорога була гарна.",
                                    "Забронював житло біля моря на кінець серпня.",
                                    "Вперше летів за кордон сам, трохи хвилювався.",
                                    "Цілий день ходили музеями, ноги гудуть.",
                                    "Знайшли затишний готель з видом на гори.",
                                    "Склав маршрут на тиждень подорожі Європою.",
                                    "Повернувся додому з купою фото і вражень.")),
                    "en", new Pool(
                            List.of("Trip", "Travel", "New city"),
                            List.of(
                                    "Took the night train and arrived early in the morning.",
                                    "Walked around the old town and found a cozy cafe.",
                                    "Booked a small apartment near the sea for August.",
                                    "The flight was delayed but the trip was worth it.",
                                    "Hiked in the mountains all day, amazing views.",
                                    "Spent the whole day visiting museums."))),
            "study", Map.of(
                    "uk", new Pool(
                            List.of("Навчання", "Університет", "Курс", "Диплом"),
                            List.of(
                                    "Сидів над дипломом до ночі, написав розділ про пошук.",
                                    "Пройшов модуль онлайн-курсу з системного дизайну.",
                                    "Науковий керівник дав зауваження до вступу.",
                                    "Готувався до екзамену з алгоритмів, повторив графи.",
                                    "Прочитав статтю про векторний пошук і ембедінги.",
                                    "Займався англійською: розбирав співбесідні питання.",
                                    "Захистив лабораторну, викладач поставив відмінно.",
                                    "Подивився лекцію про транзакції в PostgreSQL.",
                                    "Склав план навчання на наступні три місяці.",
                                    "Розібрався, як працює конкурентність у Java.")),
                    "en", new Pool(
                            List.of("Study", "Course", "Thesis"),
                            List.of(
                                    "Worked on my thesis late into the night.",
                                    "Finished a module of an online system design course.",
                                    "My supervisor left comments on the introduction.",
                                    "Read a paper about vector search and embeddings.",
                                    "Practiced English with mock interview questions.",
                                    "Prepared for the algorithms exam all evening."))),
            "sport", Map.of(
                    "uk", new Pool(
                            List.of("Тренування", "Спорт", "Біг", "Зал"),
                            List.of(
                                    "Зранку пробіжка п'ять кілометрів у парку.",
                                    "Сходив у спортзал, зробив тренування на спину.",
                                    "Пропустив тренування, бо дуже втомився після роботи.",
                                    "Пробіг свій перший напівмарафон, ледве дійшов до фінішу.",
                                    "Катались на велосипедах вздовж річки.",
                                    "Почав робити розтяжку щоранку по десять хвилин.",
                                    "Грали з друзями у футбол до темряви.",
                                    "Ходив на басейн, проплив кілометр.",
                                    "Тренер змінив програму, тепер більше кардіо.",
                                    "Після йоги відчуваю, що спина болить менше.")),
                    "en", new Pool(
                            List.of("Workout", "Running", "Gym"),
                            List.of(
                                    "Went for a five kilometer run in the park.",
                                    "Did a full body workout at the gym.",
                                    "Skipped training because I was exhausted.",
                                    "Played football with friends until dark.",
                                    "Swam a kilometer at the pool after work.",
                                    "Started stretching every morning for ten minutes."))),
            "home", Map.of(
                    "uk", new Pool(
                            List.of("Побут", "Дім", "Справи", "Прибирання"),
                            List.of(
                                    "Генеральне прибирання квартири зайняло пів дня.",
                                    "Зламалась пральна машина, викликав майстра.",
                                    "Закупився продуктами на тиждень наперед.",
                                    "Вимкнули світло на шість годин, працював з павербанком.",
                                    "Приготував борщ за маминим рецептом.",
                                    "Переставив меблі в кімнаті, стало просторіше.",
                                    "Заплатив комуналку, тарифи знову зросли.",
                                    "Купив нову лампу для робочого столу.",
                                    "Посадив на балконі розсаду помідорів.",
                                    "Сусіди знову робили ремонт з самого ранку.")),
                    "en", new Pool(
                            List.of("Home", "Chores", "Groceries"),
                            List.of(
                                    "Spent half the day cleaning the apartment.",
                                    "The washing machine broke and I called a repairman.",
                                    "Did the grocery shopping for the whole week.",
                                    "There was a power outage for six hours.",
                                    "Cooked dinner using my mom's recipe.",
                                    "Paid the utility bills, prices went up again."))));

    public static void main(String[] args) throws Exception {
        List<Entry> entries = generate();
        String json = toJson(entries);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(OUT_DIR);
        Files.write(OUT_DIR.resolve("corpus.json"), bytes);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Files.writeString(OUT_DIR.resolve("corpus.sha256"), hash + "  corpus.json\n", StandardCharsets.UTF_8);
        System.out.printf("entries=%d sha256=%s%n", entries.size(), hash);
    }

    static List<Entry> generate() {
        Random random = new Random(SEED);
        List<String> topics = TOPICS.keySet().stream().sorted().toList(); // Map.of не гарантує порядок
        Set<String> seen = new HashSet<>();
        List<Entry> result = new ArrayList<>();
        while (result.size() < SIZE) {
            String topic = topics.get(random.nextInt(topics.size()));
            String lang = random.nextDouble() < UA_SHARE ? "uk" : "en";
            Pool pool = TOPICS.get(topic).get(lang);
            LocalDate date = START.plusDays(random.nextInt(DAYS));
            String content = String.join(" ", pick(random, pool.sentences(), 2 + random.nextInt(2)));
            if (!seen.add(date + "|" + content)) {
                continue; // той самий текст за ту саму дату імпорт вважав би дублікатом
            }
            String title = random.nextInt(4) == 0 ? null : pool.titles().get(random.nextInt(pool.titles().size()));
            result.add(new Entry(null, topic, lang, date, title, content));
        }
        result.sort((a, b) -> a.date().compareTo(b.date()));
        List<Entry> numbered = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            Entry e = result.get(i);
            numbered.add(new Entry(
                    "c-%04d".formatted(i + 1), e.topic(), e.lang(), e.date(), e.title(), e.content()));
        }
        return numbered;
    }

    static List<String> pick(Random random, List<String> from, int count) {
        List<String> copy = new ArrayList<>(from);
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            picked.add(copy.remove(random.nextInt(copy.size())));
        }
        return picked;
    }

    /** JSON пишеться вручну: один файл без залежностей і стабільний порядок полів. */
    static String toJson(List<Entry> entries) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            sb.append("  {\"id\": ").append(str(e.id()))
                    .append(", \"topic\": ").append(str(e.topic()))
                    .append(", \"lang\": ").append(str(e.lang()))
                    .append(", \"entryDate\": ").append(str(e.date().toString()))
                    .append(", \"title\": ").append(e.title() == null ? "null" : str(e.title()))
                    .append(", \"content\": ").append(str(e.content()))
                    .append(i + 1 < entries.size() ? "},\n" : "}\n");
        }
        return sb.append("]\n").toString();
    }

    static String str(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
