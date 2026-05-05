import en from './en'

export default {
  ...en,
  app: {
    title: 'XC Админ',
    shortTitle: 'XC Admin'
  },
  common: {
    ...en.common,
    account: 'Аккаунт',
    password: 'Пароль',
    login: 'Войти',
    logout: 'Выйти',
    refresh: 'Обновить',
    language: 'Язык',
    profile: 'Администратор',
    noPermission: 'Нет доступа',
    comingSoon: 'Раздел в разработке',
    openMenu: 'Развернуть меню',
    closeMenu: 'Свернуть меню',
    search: 'Поиск',
    tabActions: 'Действия с вкладками',
    refreshCurrent: 'Обновить текущую',
    closeCurrent: 'Закрыть текущую',
    closeLeft: 'Закрыть слева',
    closeRight: 'Закрыть справа',
    closeOthers: 'Закрыть остальные',
    closeAll: 'Закрыть все'
  },
  login: {
    ...en.login,
    title: 'XC Админ',
    subtitle: 'Панель управления контентом, членством, классами и учебной статистикой.',
    accountPlaceholder: 'Аккаунт администратора',
    passwordPlaceholder: 'Пароль',
    accountRequired: 'Введите аккаунт',
    passwordRequired: 'Введите пароль',
    success: 'Вход выполнен'
  },
  menus: {
    dashboard: 'Главная',
    users: 'Пользователи',
    memberships: 'Членство и заказы',
    classrooms: 'Классы',
    content: 'Контент',
    reports: 'Отчеты',
    system: 'Система'
  },
  dashboard: {
    ...en.dashboard,
    title: 'Главная',
    subtitle: 'Обзор пользователей, членства, классов, контента и учебной активности.',
    groups: {
      userMembership: 'Пользователи и членство',
      orders: 'Заказы и платежи',
      classLearning: 'Классы и обучение',
      contentAssets: 'Контент'
    },
    userCount: 'Пользователи',
    activeUserCount: 'Активные',
    disabledUserCount: 'Отключенные',
    trialUserCount: 'Пробные',
    todayNewUserCount: 'Новые сегодня',
    activeMembershipCount: 'Активные члены',
    activePlanCount: 'Активные планы',
    todayOrderCount: 'Заказы сегодня',
    pendingOrderCount: 'Ожидают оплаты',
    paidOrderCount: 'Оплачены',
    todayPaidAmount: 'Оплачено сегодня',
    classCount: 'Классы',
    classMemberCount: 'Участники классов',
    pendingClassMemberCount: 'Ожидают проверки',
    todayActiveClassCount: 'Активные классы сегодня',
    todayStudyEventCount: 'Учебные действия сегодня',
    vocabListCount: 'Списки слов',
    inactiveVocabListCount: 'Отключенные списки',
    vocabItemCount: 'Слова',
    exerciseSetCount: 'Наборы упражнений',
    inactiveExerciseSetCount: 'Отключенные наборы',
    sentenceExerciseCount: 'Упражнения с фразами',
    videoMaterialCount: 'Материалы диалогов',
    inactiveVideoMaterialCount: 'Отключенные материалы',
    dialogueLineCount: 'Реплики'
  },
  content: {
    ...en.content,
    title: 'Контент',
    subtitle: 'Управление словарями, упражнениями и медиа для стабильной работы учебных клиентов.',
    tabs: {
      ...en.content.tabs,
      vocabLists: 'Списки слов',
      vocabItems: 'Слова',
      mediaAssets: 'Медиа',
      exerciseSets: 'Наборы упражнений',
      sentenceExercises: 'Фразы',
      videoMaterials: 'Материалы диалогов',
      dialogueLines: 'Реплики',
      lineVocab: 'Разбор слов'
    },
    reset: 'Сбросить',
    cancel: 'Отмена',
    submit: 'Отправить',
    saved: 'Сохранено',
    mediaKeyword: 'Поиск по исходному имени файла или URL',
    actions: {
      ...en.content.actions,
      importCsvWithTemplate: 'Импорт CSV / шаблон'
    },
    importCsvHint: 'Новые шаблоны используют понятные бизнес-названия колонок. ID записи можно оставить пустым для создания или заполнить для обновления. ID аудио/обложки можно оставить пустым и привязать позже. Дочерний контент импортируется в выбранного родителя выше. CSV должен быть в UTF-8.',
    importErrorFixHint: 'Исправьте CSV по номеру строки и названию поля ниже, затем импортируйте снова. Если часть строк уже импортирована, оставьте для повторного импорта только строки с ошибками.',
    importRequestErrorHint: 'Импорт не был выполнен. Скачайте новый шаблон, не меняйте обязательные заголовки, сохраните файл как UTF-8 CSV и повторите попытку.',
    importTraceId: 'Trace ID: {traceId}',
    columns: {
      ...en.content.columns,
      media: 'Исходный файл / ресурс'
    },
    status: {
      ...en.content.status,
      active: 'Активно',
      inactive: 'Отключено'
    },
    languages: {
      ...en.content.languages,
      zh: 'Китайский',
      ru: 'Русский',
      en: 'Английский'
    }
  },
  classrooms: {
    ...en.classrooms,
    title: 'Классы',
    subtitle: 'Управление классами, преподавателями, участниками и учебными данными.',
    createTitle: 'Создать класс',
    editTitle: 'Редактировать класс',
    detailTitle: 'Детали класса',
    membersTitle: 'Участники',
    statsTitle: 'Статистика',
    hours: '{hours} ч {minutes} мин',
    minutes: '{value} мин',
    seconds: '{value} сек'
  },
  memberships: {
    ...en.memberships,
    title: 'Членство и заказы',
    subtitle: 'Управление планами, заказами, оплатой и аудитом callback.',
    tabs: {
      ...en.memberships.tabs,
      plans: 'Планы',
      orders: 'Заказы',
      notifications: 'Callback'
    }
  },
  reports: {
    ...en.reports,
    title: 'Отчеты',
    subtitle: 'Анализ учебной активности, рейтингов и прогресса пользователей.',
    hours: '{hours} ч {minutes} мин',
    minutes: '{value} мин',
    seconds: '{value} сек'
  },
  system: {
    ...en.system,
    title: 'Система',
    subtitle: 'Роли, аккаунты администраторов, настройки и журнал операций.',
    tabs: {
      ...en.system.tabs,
      roles: 'Роли',
      admins: 'Администраторы',
      configs: 'Настройки',
      logs: 'Журнал'
    }
  },
  users: {
    ...en.users,
    title: 'Пользователи',
    subtitle: 'Студенты, статус обучения, состояние аккаунта и права доступа.',
    hours: '{hours} ч {minutes} мин',
    minutes: '{value} мин',
    seconds: '{value} сек'
  }
}
