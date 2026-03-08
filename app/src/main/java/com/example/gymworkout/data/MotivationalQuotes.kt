package com.example.gymworkout.data

object MotivationalQuotes {
    val quotes = listOf(
        // Arnold Schwarzenegger
        "The last three or four reps is what makes the muscle grow. This area of pain divides a champion from someone who is not a champion. - Arnold Schwarzenegger",
        "The mind is the limit. As long as the mind can envision the fact that you can do something, you can do it. - Arnold Schwarzenegger",
        "Strength does not come from winning. Your struggles develop your strengths. - Arnold Schwarzenegger",

        // Dwayne 'The Rock' Johnson
        "Success isn't always about greatness. It's about consistency. Consistent hard work leads to success. - Dwayne Johnson",
        "Be the hardest worker in the room. - Dwayne Johnson",
        "Wake up determined. Go to bed satisfied. - Dwayne Johnson",
        "Blood, sweat, and respect. First two you give, last one you earn. - Dwayne Johnson",

        // Muhammad Ali
        "I hated every minute of training, but I said, don't quit. Suffer now and live the rest of your life as a champion. - Muhammad Ali",
        "The fight is won or lost far away from witnesses, behind the lines, in the gym, and out there on the road. - Muhammad Ali",

        // Ronnie Coleman
        "Everybody wants to be a bodybuilder, but nobody wants to lift no heavy weights. - Ronnie Coleman",
        "Yeah buddy! Lightweight baby! - Ronnie Coleman",

        // CT Fletcher
        "It's still your set! - CT Fletcher",
        "I command you to grow! - CT Fletcher",

        // Greg Plitt
        "The difference between a successful person and others is not a lack of strength, not a lack of knowledge, but rather a lack of will. - Vince Lombardi",

        // Classic Gym Motivation
        "Your body can stand almost anything. It's your mind that you have to convince.",
        "The only bad workout is the one that didn't happen.",
        "Sore today, strong tomorrow.",
        "Don't stop when you're tired. Stop when you're done.",
        "The pain you feel today will be the strength you feel tomorrow.",
        "No pain, no gain. Shut up and train.",
        "Train insane or remain the same.",
        "Sweat is fat crying.",
        "Fall in love with taking care of yourself. Mind. Body. Spirit.",
        "Your only limit is you.",
        "Push yourself because no one else is going to do it for you.",
        "Discipline is doing what needs to be done, even if you don't want to do it.",
        "Champions train, losers complain.",
        "The gym is my therapy.",
        "Hustle for that muscle.",
        "Excuses don't burn calories.",
        "Weak is a choice. Strong is a decision.",
        "If it doesn't challenge you, it doesn't change you.",
        "Respect your body. It's the only one you get.",
        "Good things come to those who sweat.",
        "Results happen over time, not overnight. Work hard, stay consistent, and be patient.",
        "The body achieves what the mind believes.",
        "Strive for progress, not perfection.",
        "You don't have to be extreme, just consistent.",
        "Fitness is not about being better than someone else. It's about being better than you used to be.",
        "When you feel like quitting, think about why you started.",
        "A one hour workout is 4% of your day. No excuses.",
        "The iron never lies to you. Two hundred pounds is always two hundred pounds. - Henry Rollins",
        "What hurts today makes you stronger tomorrow. - Jay Cutler",
        "To be a champion, you must act like one. - Lou Ferrigno",
        "There are no shortcuts. Everything is reps, reps, reps. - Arnold Schwarzenegger",
        "Motivation is what gets you started. Habit is what keeps you going.",
        "Today I will do what others won't, so tomorrow I can do what others can't.",
        "It never gets easier, you just get stronger.",
        "Don't limit your challenges. Challenge your limits.",
        "The only person you are destined to become is the person you decide to be. - Ralph Waldo Emerson",
        "The hard days are the best because that's when champions are made.",
        "You are one workout away from a good mood.",
        "Wake up. Work out. Look hot. Kick ass.",
        "Definition of a really good workout: when you hate doing it, but you love finishing it.",
        "Your health is an investment, not an expense.",
        "Be stronger than your strongest excuse.",
        "The clock is ticking. Are you becoming the person you want to be?",
        "Every champion was once a contender that didn't give up.",
        "Obsessed is a word the lazy use to describe the dedicated.",
        "You miss 100% of the shots you don't take. - Wayne Gretzky",
        "Dead last finish is greater than did not finish, which trumps did not start.",
        "Once you learn to quit, it becomes a habit. - Vince Lombardi",
        "The resistance that you fight physically in the gym and the resistance that you fight in life can only build a strong character. - Arnold Schwarzenegger",
        "Some people want it to happen, some wish it would happen, others make it happen. - Michael Jordan",
        "Go the extra mile. It's never crowded.",
        "Success is usually the culmination of controlling failure. - Sylvester Stallone",
        "Your body is not Amazon Prime. Results don't come in 2 days.",
        "You don't get the ass you want by sitting on it.",
        "Action is the foundational key to all success. - Pablo Picasso",
        "If you still look good at the end of your workout, you didn't train hard enough.",
        "I don't count my sit-ups; I only start counting when it starts hurting because they're the only ones that count. - Muhammad Ali",
        "The successful warrior is the average man, with laser-like focus. - Bruce Lee",
        "Don't count the days, make the days count. - Muhammad Ali",
        "Pain is temporary. Quitting lasts forever. - Lance Armstrong",
        "The best project you'll ever work on is you.",
        "Iron sharpens iron. So one person sharpens another. - Proverbs 27:17",
        "You can have results or excuses, not both.",
        "The wall is there for a reason. The wall is not there to keep us out. The wall is there to give us a chance to show how badly we want something.",
        "Whether you think you can, or you think you can't -- you're right. - Henry Ford",
        "Get comfortable being uncomfortable.",
        "Suffer the pain of discipline or suffer the pain of regret.",
        "Strong people are harder to kill than weak people and more useful in general. - Mark Rippetoe",
        "I will sacrifice whatever is necessary to be the best. - J.J. Watt",
        "The purpose of training is to tighten up the slack, toughen the body, and polish the spirit. - Morihei Ueshiba"
    )

    fun getRandomQuote(): String = quotes.random()
}
