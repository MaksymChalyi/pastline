// Джерела у форматі ДСТУ 8302:2015. Ключ використовується в тексті як [@ключ].
// Номер у списку присвоюється автоматично за першою згадкою в записці.
const ACCESSED = 'дата звернення: 14.11.2026';
const web = (title, site, url) => `${title}. ${site} : вебсайт. URL: ${url} (${ACCESSED}).`;

module.exports = {
  pennebaker1997:
    'Pennebaker J. W. Writing about emotional experiences as a therapeutic process. Psychological Science. 1997. Vol. 8, No. 3. P. 162–166. DOI: 10.1111/j.1467-9280.1997.tb00403.x.',
  jones2008:
    'Jones W. Keeping found things found: the study and practice of personal information management. Burlington : Morgan Kaufmann, 2008. 430 p.',
  teevan2007:
    'Teevan J., Adar E., Jones R., Potts M. A. S. Information re-retrieval: repeat queries in Yahoo’s logs. Proceedings of the 30th Annual International ACM SIGIR Conference on Research and Development in Information Retrieval. Amsterdam, 2007. P. 151–158. DOI: 10.1145/1277741.1277770.',
  dumais2003:
    'Dumais S., Cutrell E., Cadiz J. J., Jancke G., Sarin R., Robbins D. C. Stuff I’ve seen: a system for personal information retrieval and re-use. Proceedings of the 26th Annual International ACM SIGIR Conference on Research and Development in Information Retrieval. Toronto, 2003. P. 72–79. DOI: 10.1145/860435.860451.',
  elsweiler2007:
    'Elsweiler D., Ruthven I., Jones C. Towards memory supporting personal information management tools. Journal of the American Society for Information Science and Technology. 2007. Vol. 58, No. 7. P. 924–946. DOI: 10.1002/asi.20570.',
  furnas1987:
    'Furnas G. W., Landauer T. K., Gomez L. M., Dumais S. T. The vocabulary problem in human-system communication. Communications of the ACM. 1987. Vol. 30, No. 11. P. 964–971. DOI: 10.1145/32206.32212.',
  karpukhin2020:
    'Karpukhin V., Oğuz B., Min S., Lewis P., Wu L., Edunov S., Chen D., Yih W. Dense passage retrieval for open-domain question answering. Proceedings of the 2020 Conference on Empirical Methods in Natural Language Processing (EMNLP). 2020. P. 6769–6781. DOI: 10.18653/v1/2020.emnlp-main.550.',
  lewis2020:
    'Lewis P., Perez E., Piktus A., Petroni F., Karpukhin V., Goyal N. et al. Retrieval-augmented generation for knowledge-intensive NLP tasks. Advances in Neural Information Processing Systems 33 (NeurIPS 2020). 2020. URL: https://arxiv.org/abs/2005.11401 (' + ACCESSED + ').',
  thakur2021:
    'Thakur N., Reimers N., Rücklé A., Srivastava A., Gurevych I. BEIR: a heterogeneous benchmark for zero-shot evaluation of information retrieval models. Proceedings of the Neural Information Processing Systems Track on Datasets and Benchmarks. 2021. URL: https://arxiv.org/abs/2104.08663 (' + ACCESSED + ').',
  ji2023:
    'Ji Z., Lee N., Frieske R., Yu T., Su D., Xu Y. et al. Survey of hallucination in natural language generation. ACM Computing Surveys. 2023. Vol. 55, No. 12. Article 248. DOI: 10.1145/3571730.',
  cormack2009:
    'Cormack G. V., Clarke C. L. A., Büttcher S. Reciprocal rank fusion outperforms Condorcet and individual rank learning methods. Proceedings of the 32nd International ACM SIGIR Conference on Research and Development in Information Retrieval. Boston, 2009. P. 758–759. DOI: 10.1145/1571941.1572114.',

  dayone_pricing: web('Choose a Day One plan', 'Day One', 'https://dayoneapp.com/pricing'),
  dayone_search: web('Search in Day One', 'Day One Help Guides', 'https://dayoneapp.com/guides/tips-and-tutorials/search-in-day-one/'),
  dayone_release: web('iOS release notes', 'Day One Help Guides', 'https://dayoneapp.com/guides/release-notes/ios-release-notes/'),
  apple_journal_newsroom: web('Apple launches Journal app, a new app for reflecting on everyday moments', 'Apple Newsroom', 'https://www.apple.com/newsroom/2023/12/apple-launches-journal-app-a-new-app-for-reflecting-on-everyday-moments/'),
  apple_journal_privacy: web('Journaling Suggestions & Privacy', 'Apple', 'https://www.apple.com/legal/privacy/data/en/journaling-suggestions/'),
  apple_journal_search: web('View and search your journal entries on Mac', 'Journal User Guide. Apple Support', 'https://support.apple.com/guide/journal/dev153edfa89/mac'),
  journey_site: web('Journey — your private diary and journal app', 'Two App Studio', 'https://journey.cloud/'),
  journey_appstore: web('Journey — Diary, Journal', 'App Store', 'https://apps.apple.com/app/id1300202543'),
  journey_encryption: web('How does encryption work in Journey', 'Journey Support', 'https://support.journey.cloud/categories/synchronization-cloud/articles/how-does-encryption-work-in-journey'),
  journey_e2ee: web('End-to-end encryption', 'Journey Help Center', 'https://help.journey.cloud/en/article/end-to-end-encryption-cirdzr'),
  journey_odyssey: web('Odyssey AI', 'Journey Support', 'https://support.journey.cloud/en/categories/web-services/articles/odyssey-ai'),
  rosebud_ask: web('Ask Rosebud', 'Rosebud Knowledge Base', 'https://help.rosebud.app/tools-for-growth/ask-rosebud'),
  rosebud_free: web('Changes to the free plan', 'Rosebud Knowledge Base', 'https://help.rosebud.app/account/changes-to-the-free-plan'),
  rosebud_privacy: web('Privacy policy', 'Rosebud Knowledge Base', 'https://help.rosebud.app/about-us/privacy-policy'),
  rosebud_appstore: web('Rosebud: AI Journal & Diary', 'App Store', 'https://apps.apple.com/us/app/id6451135127'),
  reflection_faq: web('How can we help? (FAQ)', 'Reflection', 'https://www.reflection.app/faq'),
  notion_qna: web('Q&A', 'Notion Help Center', 'https://www.notion.com/help/qna'),
  obsidian_smart_connections: web('Smart Connections', 'Obsidian Community Plugins', 'https://community.obsidian.md/plugins/smart-connections'),
  reflection_appstore: web('Reflection: AI Journal & Coach', 'App Store', 'https://apps.apple.com/app/id1504547616'),
  // ---------- розділ 2 ----------
  sparckjones1972:
    'Spärck Jones K. A statistical interpretation of term specificity and its application in retrieval. Journal of Documentation. 1972. Vol. 28, No. 1. P. 11–21. DOI: 10.1108/eb026526.',
  manning2008:
    'Manning C. D., Raghavan P., Schütze H. Introduction to information retrieval. Cambridge : Cambridge University Press, 2008. 482 p.',
  robertson2009:
    'Robertson S., Zaragoza H. The probabilistic relevance framework: BM25 and beyond. Foundations and Trends in Information Retrieval. 2009. Vol. 3, No. 4. P. 333–389. DOI: 10.1561/1500000019.',
  pg_textsearch: web('PostgreSQL documentation. Chapter 12. Full Text Search. 12.3. Controlling Text Search', 'PostgreSQL Global Development Group', 'https://www.postgresql.org/docs/current/textsearch-controls.html'),
  reimers2019:
    'Reimers N., Gurevych I. Sentence-BERT: sentence embeddings using Siamese BERT-networks. Proceedings of the 2019 Conference on Empirical Methods in Natural Language Processing and the 9th International Joint Conference on Natural Language Processing (EMNLP-IJCNLP). Hong Kong, 2019. P. 3982–3992. DOI: 10.18653/v1/D19-1410.',
  embeddinggemma2025:
    'Schechter Vera H., Dua S. et al. EmbeddingGemma: powerful and lightweight text representations. arXiv preprint. 2025. arXiv:2509.20354. URL: https://arxiv.org/abs/2509.20354 (' + ACCESSED + ').',
  embeddinggemma_card: web('EmbeddingGemma model card', 'Google AI for Developers', 'https://ai.google.dev/gemma/docs/embeddinggemma/model_card'),
  malkov2020:
    'Malkov Y. A., Yashunin D. A. Efficient and robust approximate nearest neighbor search using hierarchical navigable small world graphs. IEEE Transactions on Pattern Analysis and Machine Intelligence. 2020. Vol. 42, No. 4. P. 824–836. DOI: 10.1109/TPAMI.2018.2889473.',
  pgvector: web('pgvector: open-source vector similarity search for Postgres', 'GitHub', 'https://github.com/pgvector/pgvector'),
  greshake2023:
    'Greshake K., Abdelnabi S., Mishra S., Endres C., Holz T., Fritz M. Not what you’ve signed up for: compromising real-world LLM-integrated applications with indirect prompt injection. Proceedings of the 16th ACM Workshop on Artificial Intelligence and Security (AISec ’23). Copenhagen, 2023. P. 79–90. DOI: 10.1145/3605764.3623985.',
  voorhees1999:
    'Voorhees E. M. The TREC-8 question answering track report. The Eighth Text REtrieval Conference (TREC-8) : NIST Special Publication 500-246. Gaithersburg, 2000. P. 77–82.',
  fowler2015: web('Fowler M. MonolithFirst', 'martinfowler.com', 'https://martinfowler.com/bliki/MonolithFirst.html'),
  cockburn2005: web('Cockburn A. Hexagonal architecture (ports and adapters). HaT Technical Report 2005.02', 'Alistair Cockburn', 'https://alistair.cockburn.us/hexagonal-architecture/'),
  jdk21: web('JDK 21', 'OpenJDK', 'https://openjdk.org/projects/jdk/21/'),
  spring_boot4: web('Spring Framework 7 and Spring Boot 4 deliver API versioning, resilience, and null-safe annotations', 'InfoQ', 'https://www.infoq.com/news/2025/11/spring-7-spring-boot-4/'),
  spring_security_csrf: web('Cross Site Request Forgery (CSRF). Spring Security reference', 'Spring', 'https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html'),
  owasp_csrf: web('Cross-Site Request Forgery Prevention Cheat Sheet', 'OWASP Cheat Sheet Series', 'https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html'),
  spring_ai: web('Spring AI reference', 'Spring', 'https://docs.spring.io/spring-ai/reference/'),
  langchain4j: web('LangChain4j documentation', 'LangChain4j', 'https://docs.langchain4j.dev/'),
  qwen3_report:
    'Yang A., Li A., Yang B. et al. Qwen3 technical report. arXiv preprint. 2025. arXiv:2505.09388. URL: https://arxiv.org/abs/2505.09388 (' + ACCESSED + ').',
  qwen3_card: web('Qwen3-4B-Instruct-2507', 'Hugging Face', 'https://huggingface.co/Qwen/Qwen3-4B-Instruct-2507'),
  ollama: web('Ollama documentation', 'Ollama', 'https://docs.ollama.com/'),
  gemini_terms: web('Gemini API Additional Terms of Service', 'Google AI for Developers', 'https://ai.google.dev/gemini-api/terms'),
  qdrant: web('Qdrant documentation', 'Qdrant', 'https://qdrant.tech/documentation/'),
  milvus: web('Milvus documentation', 'Milvus', 'https://milvus.io/docs'),
  elastic_knn: web('kNN search. Elasticsearch guide', 'Elastic', 'https://www.elastic.co/docs/solutions/search/vector/knn'),
  flyway: web('Flyway documentation', 'Redgate', 'https://documentation.red-gate.com/flyway'),
  testcontainers: web('Testcontainers for Java', 'Testcontainers', 'https://java.testcontainers.org/'),
  react: web('React reference', 'Meta Open Source', 'https://react.dev/reference/react'),
  typescript: web('TypeScript documentation', 'Microsoft', 'https://www.typescriptlang.org/docs/'),
  vite: web('Vite guide', 'Vite', 'https://vite.dev/guide/'),
  tanstack_query: web('TanStack Query. Overview', 'TanStack', 'https://tanstack.com/query/latest/docs/framework/react/overview'),
  docker_compose: web('Docker Compose overview', 'Docker Docs', 'https://docs.docker.com/compose/'),
  render_free: web('Deploy for free', 'Render Docs', 'https://render.com/docs/free'),
  vercel_rewrites: web('Rewrites on Vercel', 'Vercel Docs', 'https://vercel.com/docs/routing/rewrites'),
  neon_pgvector: web('The pgvector extension', 'Neon Docs', 'https://neon.com/docs/extensions/pgvector'),
  pg_dictionaries: web('PostgreSQL documentation. Chapter 12. Full Text Search. 12.6. Dictionaries', 'PostgreSQL Global Development Group', 'https://www.postgresql.org/docs/current/textsearch-dictionaries.html'),
  e5_card: web('intfloat/multilingual-e5-large', 'Hugging Face', 'https://huggingface.co/intfloat/multilingual-e5-large'),
  wang2024e5:
    'Wang L., Yang N., Huang X., Yang L., Majumder R., Wei F. Multilingual E5 text embeddings: a technical report. arXiv preprint. 2024. arXiv:2402.05672. URL: https://arxiv.org/abs/2402.05672 (' + ACCESSED + ').',
  bgem3_card: web('BAAI/bge-m3', 'Hugging Face', 'https://huggingface.co/BAAI/bge-m3'),
};
