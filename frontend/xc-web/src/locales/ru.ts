export default {
  app: {
    title: 'Изучение китайского'
  },
  language: {
    label: 'Язык интерфейса',
    zhCN: '简体中文',
    en: 'English',
    ru: 'Русский'
  },
  common: {
    refresh: 'Обновить',
    logout: 'Выйти',
    back: 'Назад',
    available: 'Доступно',
    memberRequired: 'Для подписки',
    comingSoon: 'Скоро будет',
    russian: 'Русский',
    english: 'Английский',
    basic: 'Базовый',
    confirm: 'Подтвердите действие',
    userFallback: 'Пользователь {id}'
  },
  units: {
    zeroDays: '0 дн.',
    days: '{count} дн.',
    durationHoursMinutes: '{hours} ч {minutes} мин',
    durationMinutes: '{minutes} мин'
  },
  api: {
    baseUrlMissing: 'VITE_API_BASE_URL не настроен',
    requestFailed: 'Ошибка запроса'
  },
  login: {
    mark: '学',
    eyebrow: 'Вход для учеников, изучающих китайский',
    title: 'Изучение китайского',
    subtitle: 'Занимайтесь понемногу каждый день и превращайте язык в навык.',
    panelTitle: 'Начните занятие сегодня',
    panelSubtitle: 'Войдите в аккаунт или зарегистрируйтесь для 7-дневного пробного периода.',
    loginTab: 'Вход',
    registerTab: 'Регистрация',
    email: 'Email',
    emailPlaceholder: "student{'@'}example.com",
    nickname: 'Имя',
    nicknamePlaceholder: 'Ваше имя ученика',
    password: 'Пароль',
    passwordPlaceholder: 'Введите пароль',
    enter: 'Начать обучение',
    registerStart: 'Начать 7-дневный пробный период',
    trialNote: 'Пробный период включает слова, практику предложений, классы и историю обучения.',
    previewLesson: 'Практика сегодня',
    previewLevel: 'HSK базовый',
    previewHanzi: '你好',
    previewPinyin: 'nǐ hǎo',
    previewMeaning: 'Привет',
    statStreak: 'Серия',
    statStreakValue: '7 дн.',
    statAccuracy: 'Точность',
    statAccuracyValue: '92%',
    statWords: 'Слов изучено',
    statWordsValue: '180',
    benefitListen: 'Тренируйте аудио, пиньинь, иероглифы и значения вместе',
    benefitPractice: 'Практика предложений и словарь синхронизируются автоматически',
    benefitClass: 'Поддерживаются классы, проверка учителем и статистика',
    validEmail: 'Введите корректный email',
    passwordRequired: 'Введите пароль',
    passwordLength: 'Пароль должен быть от 8 до 72 символов',
    nicknameLength: 'Имя должно быть не длиннее 100 символов',
    loginSuccess: 'Вход выполнен',
    trialStarted: 'Пробный период на 7 дней активирован'
  },
  home: {
    welcome: '{name}, продолжим занятия сегодня.',
    learner: 'Ученик',
    accessStatus: 'Доступ',
    remainingTime: 'Осталось',
    currentStreak: 'Серия',
    accuracy: 'Точность',
    vocabLists: 'Списки слов',
    myClasses: 'Мои классы',
    emptyVocabLists: 'Нет списков слов',
    emptyClasses: 'Нет классов',
    table: {
      name: 'Название',
      level: 'Уровень',
      type: 'Тип',
      className: 'Класс',
      role: 'Роль',
      inviteCode: 'Код приглашения'
    },
    access: {
      member: 'Подписка',
      trial: 'Пробный период',
      free: 'Бесплатно'
    },
    featureLocked: 'Для этой функции нужен пробный период или подписка',
    features: {
      vocab: {
        title: 'Слова',
        description: 'HSK, YCT, тематические списки и избранное.'
      },
      practice: {
        title: 'Практика предложений',
        description: 'Аудио-порядок, диктант и задания по пиньиню.'
      },
      dialogue: {
        title: 'Тренировка диалогов',
        description: 'Слушайте, повторяйте, записывайте и проверяйте речь.'
      },
      matching: {
        title: 'Игра на соответствие',
        description: 'Соединяйте китайские слова с русскими или английскими значениями.'
      },
      classroom: {
        title: 'Классы',
        description: 'Участники класса и статистика обучения.'
      },
      records: {
        title: 'История обучения',
        description: 'Серии, точность, прогресс и рейтинги.'
      }
    }
  },
  vocab: {
    title: 'Слова',
    empty: 'Нет слов',
    showPinyin: 'Показать пиньинь',
    hidePinyin: 'Скрыть пиньинь',
    showMeaning: 'Показать значение',
    showHanzi: 'Показать иероглифы',
    favorite: 'В избранное',
    unfavorite: 'Убрать из избранного',
    playAudio: 'Воспроизвести',
    previous: 'Предыдущее',
    next: 'Следующее',
    favorited: 'Добавлено в избранное',
    unfavorited: 'Удалено из избранного'
  },
  practice: {
    title: 'Практика предложений',
    selectSet: 'Выберите набор заданий',
    noSets: 'Нет наборов заданий',
    playAudio: 'Воспроизвести',
    answerPlaceholder: 'Введите ответ на китайском',
    standardAnswer: 'Правильный ответ: {answer}',
    submit: 'Отправить',
    showAnswer: 'Показать ответ',
    previous: 'Предыдущее',
    next: 'Следующее',
    changeSet: 'Сменить набор',
    selectWordsWarning: 'Выберите блоки слов',
    answerRequired: 'Введите ответ',
    types: {
      audio_order: 'Порядок по аудио',
      audio_dictation: 'Аудиодиктант',
      pinyin_dictation: 'Диктант по пиньиню',
      translation_order: 'Порядок по переводу'
    }
  },
  records: {
    title: 'История обучения',
    subtitle: 'Просматривайте недавние действия, ежедневную статистику и общий прогресс.',
    totalStudyTime: 'Общее время',
    exerciseCount: 'Упражнения',
    accuracy: 'Точность',
    longestStreak: 'Лучшая серия',
    dailyStats: 'Ежедневная статистика',
    events: 'Действия',
    allTypes: 'Все типы',
    emptyDailyStats: 'Нет ежедневной статистики',
    emptyEvents: 'Нет действий',
    table: {
      date: 'Дата',
      studyTime: 'Время',
      exercises: 'Задания',
      correct: 'Верно',
      vocab: 'Слова',
      accuracy: 'Точность',
      type: 'Тип',
      result: 'Результат',
      duration: 'Длительность',
      time: 'Время'
    },
    eventTypes: {
      exercise: 'Упражнение',
      vocab: 'Слова',
      dialogue: 'Диалог',
      matching_game: 'Игра'
    },
    results: {
      correct: 'Верно',
      wrong: 'Неверно',
      completed: 'Завершено'
    }
  },
  classroom: {
    title: 'Классы',
    subtitle: 'Создавайте классы, вступайте в них и смотрите статистику участников.',
    createName: 'Название класса',
    createDescription: 'Описание класса',
    create: 'Создать',
    inviteCode: 'Код приглашения',
    join: 'Вступить',
    emptyRooms: 'Нет классов',
    chooseRoom: 'Выберите класс',
    noDescription: 'Нет описания',
    activeMembers: 'Активные участники',
    pendingMembers: 'Ожидают',
    myRole: 'Моя роль',
    inactiveAlert: 'Ваш статус участника еще не активен, поэтому участники и статистика недоступны.',
    addByEmail: 'Добавить по email',
    addByUserId: 'Или ID пользователя',
    addMember: 'Добавить / отправить запрос',
    members: 'Участники',
    stats: 'Статистика обучения',
    table: {
      member: 'Участник',
      role: 'Роль',
      status: 'Статус',
      actions: 'Действия',
      studyTime: 'Время',
      exercises: 'Задания',
      correct: 'Верно',
      accuracy: 'Точность',
      vocab: 'Слова',
      lastStudy: 'Последнее занятие'
    },
    actions: {
      approve: 'Принять',
      reject: 'Отклонить',
      remove: 'Удалить'
    },
    notifications: {
      nameRequired: 'Введите название класса',
      created: 'Класс создан',
      inviteRequired: 'Введите код приглашения',
      joined: 'Вы вступили в класс',
      memberRequired: 'Введите email или ID пользователя',
      memberHandled: 'Участник обработан',
      approved: 'Принято',
      rejected: 'Отклонено',
      removed: 'Удалено'
    },
    confirmRemoveTitle: 'Удалить участника',
    confirmRemoveMessage: 'Удалить этого участника?',
    roles: {
      teacher: 'Учитель',
      member: 'Участник'
    },
    statuses: {
      active: 'Активен',
      pending_teacher_review: 'Ожидает проверки',
      invited: 'Приглашен',
      rejected: 'Отклонен',
      left: 'Вышел',
      removed: 'Удален'
    }
  }
}
