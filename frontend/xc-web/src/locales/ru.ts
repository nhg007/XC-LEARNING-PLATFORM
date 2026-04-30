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
    membership: 'Подписка',
    referenceLanguage: 'Язык подсказки',
    confirm: 'Подтвердите действие',
    yes: 'Да',
    no: 'Нет',
    userFallback: 'Пользователь {id}',
    studyLanguages: {
      zh: 'Китайский',
      ru: 'Русский',
      en: 'Английский'
    }
  },
  units: {
    zeroDays: '0 дн.',
    days: '{count} дн.',
    durationHoursMinutes: '{hours} ч {minutes} мин',
    durationMinutes: '{minutes} мин'
  },
  api: {
    baseUrlMissing: 'VITE_API_BASE_URL не настроен',
    requestFailed: 'Ошибка запроса',
    membershipRequired: 'Для этой функции нужен пробный период или подписка'
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
    benefitClass: 'Вступайте в классы и смотрите участников',
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
      favorites: {
        title: 'Избранное',
        description: 'Смотрите сохраненные слова и возвращайтесь к исходному списку.'
      },
      practice: {
        title: 'Практика предложений',
        description: 'Аудио-порядок, диктант и задания по пиньиню.'
      },
      dialogue: {
        title: 'Тренировка диалогов',
        description: 'Тренируйте порядок реплик, диктант и повторение.'
      },
      matching: {
        title: 'Игра на соответствие',
        description: 'Соединяйте материал целевого языка со значениями на языке подсказки.'
      },
      elimination: {
        title: 'Мемори',
        description: 'Китайские слова и значения смешаны как карточки. Найдите пары и уберите их.'
      },
      classroom: {
        title: 'Классы',
        description: 'Вступайте в классы и смотрите участников.'
      },
      records: {
        title: 'История обучения',
        description: 'Серии, точность, прогресс и рейтинги.'
      }
    }
  },
  vocab: {
    title: 'Слова',
    favorites: 'Избранное',
    favoritesTitle: 'Избранное',
    favoritesSubtitle: 'Сохраненные слова в одном месте для быстрого повторения и игры на соответствие.',
    favoriteCount: 'Сохранено слов: {count}',
    emptyFavorites: 'Сохраненных слов пока нет. Нажмите «В избранное» во время повторения слов.',
    browseLists: 'Списки слов',
    openList: 'Открыть список',
    empty: 'Нет слов',
    showPinyin: 'Показать пиньинь',
    hidePinyin: 'Скрыть пиньинь',
    showMeaning: 'Показать значение',
    showHanzi: 'Показать иероглифы',
    favorite: 'В избранное',
    unfavorite: 'Убрать из избранного',
    playPronunciation: 'Произношение',
    playAudio: 'Воспроизвести значение',
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
  dialogue: {
    title: 'Тренировка диалогов',
    subtitle: 'Выберите материал и тренируйте порядок, диктант и повторение по репликам.',
    noMaterials: 'Нет материалов',
    noLines: 'Нет реплик',
    noDescription: 'Нет описания',
    noTranslation: 'Нет перевода',
    noVocabAnalysis: 'Нет разбора слов',
    lineCount: '{count} реплик',
    changeMaterial: 'Сменить материал',
    currentLine: 'Реплика {current} / {total}',
    translationPrompt: 'Подсказка перевода',
    playAudio: 'Воспроизвести реплику',
    audioPending: 'Аудио пока нет',
    answerPlaceholder: 'Введите китайскую реплику',
    selectWordsHint: 'Нажимайте символы ниже, чтобы собрать реплику',
    selectWordsWarning: 'Выберите символы',
    answerRequired: 'Введите реплику',
    submit: 'Проверить',
    showAnswer: 'Показать ответ',
    standardAnswer: 'Ответ: {answer}',
    previous: 'Назад',
    next: 'Далее',
    completeShadow: 'Завершить повтор',
    shadowHint: 'Прослушайте или прочитайте реплику, затем повторите вслух. ASR-оценка будет подключена позже.',
    analysis: 'Ответ и разбор',
    analysisHint: 'Отправьте ответ или покажите его, чтобы увидеть перевод и разбор слов.',
    types: {
      all: 'Все',
      drama: 'Сериал',
      short_video: 'Короткое видео',
      cartoon: 'Мультфильм'
    },
    modes: {
      order: 'Порядок',
      dictation: 'Диктант',
      shadow: 'Повтор'
    }
  },
  matching: {
    title: 'Игра на соответствие',
    subtitle: 'Выберите источник слов, язык подсказки и сложность, затем соедините материал на языке «{target}» со значением на языке «{reference}».',
    playingSubtitle: 'Выберите материал на языке «{target}» слева и значение на языке «{reference}» справа.',
    newGame: 'Новая игра',
    settings: 'Настройки игры',
    source: 'Источник',
    vocabList: 'Список слов',
    sound: 'Звук',
    start: 'Начать игру',
    rulesTitle: 'Правила',
    rulePairs: 'Пары',
    rulePairsText: 'У каждого элемента на языке «{target}» есть одно значение на языке «{reference}». Ошибки считаются.',
    ruleStats: 'Запись',
    ruleStatsText: 'Когда все пары найдены, результат попадет в статистику обучения.',
    progress: 'Прогресс',
    errors: 'Ошибки',
    elapsed: 'Время',
    difficulty: 'Сложность',
    targetColumn: '{language}',
    referenceColumn: 'Значение: {language}',
    completedTitle: 'Готово',
    completedText: 'Время {time}, ошибок: {errors}.',
    completedToast: 'Игра завершена',
    selectListWarning: 'Выберите список слов',
    selectStageWarning: 'Выберите уровень',
    saveFailed: 'Не удалось сохранить запись',
    sources: {
      vocab_list: 'Список слов',
      favorites: 'Избранное'
    },
    difficulties: {
      '4x4': '4 пары',
      '7x7': '7 пар',
      '10x10': '10 пар'
    }
  },
  elimination: {
    title: 'Мемори',
    subtitle: 'Выберите источник слов и уровень, затем убирайте пары китайских слов и значений.',
    playerBest: 'Лучший результат',
    points: 'баллы',
    setup: 'Выбор уровня',
    setupDesc: 'Выберите источник слов и группу, затем уровень. Для каждого уровня можно задать свое число карточек.',
    stageGroups: 'Группы уровней',
    levels: 'Уровни',
    levelNumber: 'Уровень {number}',
    levelCount: 'Уровней: {count}',
    cards: 'Карточек: {count}',
    limit: 'Лимит {time}',
    remaining: 'Осталось',
    locked: 'Закрыто',
    lockedWarning: 'Сначала пройдите предыдущий уровень',
    bestTime: 'Лучшее {time}',
    cleared: 'Убрано',
    combo: 'Комбо',
    boardHint: 'Выбирайте две карточки за ход. Китайская карточка и правильное значение исчезнут вместе.',
    completedTitle: 'Уровень пройден',
    completedSummary: 'Найдено пар: {pairs}, время {time}, ошибок: {wrong}.',
    failedTitle: 'Время вышло',
    failedSummary: 'Найдено пар: {pairs}/{total}. Повторите и попробуйте снова.'
  },
  membership: {
    title: 'Подписка',
    heroTitle: 'Откройте практику, речь и повторение полностью',
    subtitle: 'Выберите темп обучения и получите полный доступ к упражнениям, диалогам, игре на сопоставление и истории прогресса.',
    currentAccess: 'Текущий доступ',
    accessLevel: 'Доступ',
    remaining: 'Осталось',
    trialEndsAt: 'Пробный период до',
    membershipEndsAt: 'Подписка до',
    plans: 'Тарифы',
    plansHint: 'В первом релизе доступны WeChat Pay и Alipay. В dev-режиме можно использовать имитацию оплаты.',
    emptyPlans: 'Нет доступных тарифов',
    bestValue: 'Рекомендуем',
    fullAccessBenefit: 'Полный доступ к обучению',
    validFor: 'Действует {count} дн.',
    payWith: 'Через {provider}',
    checkoutLabel: 'Оплата',
    checkoutEmptyTitle: 'Сначала выберите тариф',
    durationDays: '{count} дн. доступа',
    orderTitle: 'Заказ оплаты',
    noOrder: 'Выберите тариф и способ оплаты, чтобы увидеть заказ и статус.',
    orderNo: 'Номер заказа',
    orderPlan: 'Тариф',
    orderAmount: 'Сумма',
    orderProvider: 'Способ оплаты',
    orderStatus: 'Статус',
    paymentLink: 'Ссылка оплаты',
    mockNotice: 'Включена dev-имитация оплаты. Реального списания не будет.',
    simulatePaid: 'Имитировать оплату',
    refreshOrder: 'Обновить заказ',
    createSuccess: 'Заказ создан',
    paidSuccess: 'Подписка активирована',
    duration: {
      day: '{count} дн.',
      month: '{count} мес.',
      custom: '{days} дн.'
    },
    providers: {
      wechat_pay: 'WeChat Pay',
      alipay: 'Alipay'
    },
    benefits: {
      practice: 'Практика фраз',
      dialogue: 'Диалоги',
      records: 'Повторение',
      classroom: 'Класс'
    },
    orderStatuses: {
      pending: 'Ожидает оплаты',
      paid: 'Оплачен',
      failed: 'Ошибка',
      refunded: 'Возврат'
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
    attempts: 'Ответы',
    leaderboards: 'Рейтинг',
    leaderboardPeriod: 'Период',
    leaderboardMetric: 'Показатель',
    leaderboardRefresh: 'Обновить рейтинг',
    allTypes: 'Все типы',
    emptyDailyStats: 'Нет ежедневной статистики',
    emptyEvents: 'Нет действий',
    emptyAttempts: 'Нет ответов',
    emptyLeaderboards: 'Нет данных рейтинга',
    reports: {
      studyTrend: 'Динамика времени',
      accuracyTrend: 'Динамика точности',
      activityMix: 'Структура практики',
      recentDays: 'За {count} дн.',
      totalItems: 'Всего {count}',
      noData: 'Нет данных для отчета'
    },
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
      time: 'Время',
      answer: 'Ответ',
      referenceLanguage: 'Язык подсказки',
      showedAnswer: 'Смотрел ответ',
      rank: 'Место',
      user: 'Ученик',
      metric: 'Показатель',
      score: 'Баллы',
      period: 'Период',
      generatedAt: 'Создано'
    },
    eventTypes: {
      exercise: 'Упражнение',
      vocab: 'Слова',
      dialogue: 'Диалог',
      matching_game: 'Игра'
    },
    leaderboardPeriods: {
      daily: 'День',
      weekly: 'Неделя',
      monthly: 'Месяц',
      all: 'За все время'
    },
    leaderboardMetrics: {
      streak: 'Серия',
      accuracy: 'Точность',
      vocab_count: 'Слова',
      game_score: 'Игровые баллы'
    },
    results: {
      correct: 'Верно',
      wrong: 'Неверно',
      completed: 'Завершено'
    }
  },
  classroom: {
    title: 'Классы',
    subtitle: 'Вступайте в классы и просматривайте информацию о своем классе и участниках.',
    joinTitle: 'Вступить в класс',
    inviteCode: 'Код приглашения',
    join: 'Вступить',
    emptyRooms: 'Нет классов',
    chooseRoom: 'Выберите класс',
    noDescription: 'Нет описания',
    activeMembers: 'Активные участники',
    pendingMembers: 'Ожидают',
    myRole: 'Моя роль',
    inactiveAlert: 'Ваш статус участника еще не активен, поэтому информация об участниках недоступна.',
    members: 'Участники',
    table: {
      member: 'Участник',
      role: 'Роль',
      status: 'Статус'
    },
    notifications: {
      inviteRequired: 'Введите код приглашения',
      joined: 'Вы вступили в класс'
    },
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
