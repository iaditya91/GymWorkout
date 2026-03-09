const fs = require('fs');

const data = JSON.parse(fs.readFileSync('app/src/main/res/raw/exercises.json', 'utf8'));

// Helper: lowercase name for matching
function n(exercise) { return exercise.name.toLowerCase(); }
function hasPrimary(exercise, muscle) { return exercise.primaryMuscles.includes(muscle); }
function hasSecondary(exercise, muscle) { return (exercise.secondaryMuscles || []).includes(muscle); }
function has(exercise, muscle) { return hasPrimary(exercise, muscle) || hasSecondary(exercise, muscle); }

// Keyword helpers
function nameHas(exercise, ...keywords) {
  const nm = n(exercise);
  return keywords.some(k => nm.includes(k.toLowerCase()));
}

function nameHasAll(exercise, ...keywords) {
  const nm = n(exercise);
  return keywords.every(k => nm.includes(k.toLowerCase()));
}

// ==================== SUB-TARGET MAPPING FUNCTIONS ====================

function getChestSubTargets(exercise) {
  const nm = n(exercise);

  // Incline = upper chest
  if (nameHas(exercise, 'incline')) return ['upper chest'];
  // Decline = lower chest
  if (nameHas(exercise, 'decline')) return ['lower chest'];
  // Guillotine press targets upper chest
  if (nameHas(exercise, 'guillotine')) return ['upper chest'];
  // Flyes, crossover, cable crossover = inner chest emphasis
  if (nameHas(exercise, 'fly', 'flye', 'flie', 'crossover', 'cross-over', 'pec deck', 'butterfly')) return ['inner chest', 'mid chest'];
  // Squeeze press
  if (nameHas(exercise, 'squeeze')) return ['inner chest', 'mid chest'];
  // Dips = lower chest
  if (nameHas(exercise, 'dip') && !nameHas(exercise, 'tricep')) return ['lower chest', 'mid chest'];
  // Push-ups variations
  if (nameHas(exercise, 'push-up', 'pushup', 'push up')) {
    if (nameHas(exercise, 'decline')) return ['upper chest']; // decline push-up = feet elevated = upper chest
    if (nameHas(exercise, 'incline')) return ['lower chest']; // incline push-up = hands elevated = lower chest
    if (nameHas(exercise, 'diamond', 'close', 'narrow')) return ['inner chest', 'mid chest'];
    if (nameHas(exercise, 'wide')) return ['outer chest', 'mid chest'];
    return ['mid chest'];
  }
  // Flat bench press variations
  if (nameHas(exercise, 'bench press', 'floor press', 'chest press')) return ['mid chest'];
  // Pullover targets lower/inner chest when chest is primary
  if (nameHas(exercise, 'pullover')) return ['lower chest', 'inner chest'];
  // Around the worlds
  if (nameHas(exercise, 'around the world')) return ['upper chest', 'mid chest'];
  // Default flat/mid chest
  return ['mid chest'];
}

function getShoulderSubTargets(exercise) {
  const nm = n(exercise);

  // Rear delt specific
  if (nameHas(exercise, 'rear delt', 'reverse fly', 'reverse flye', 'face pull', 'band pull apart')) return ['rear delts'];
  if (nameHas(exercise, 'back flye', 'back fly', 'rear lateral')) return ['rear delts'];
  if (nameHas(exercise, 'bent-over raise', 'bent over raise', 'bent-over lateral', 'bent over lateral')) return ['rear delts'];
  if (nameHas(exercise, 'prone')) return ['rear delts'];

  // Front delt specific
  if (nameHas(exercise, 'front raise', 'front delt', 'anterior')) return ['front delts'];
  if (nameHas(exercise, 'bus driver')) return ['front delts', 'side delts'];

  // Lateral/side delt specific
  if (nameHas(exercise, 'lateral raise', 'side raise', 'side lateral', 'deltoid raise', 'side delt')) return ['side delts'];
  if (nameHas(exercise, 'upright row')) return ['side delts', 'front delts'];
  if (nameHas(exercise, 'car driver')) return ['front delts', 'side delts'];
  if (nameHas(exercise, 'iron cross')) return ['side delts'];
  if (nameHas(exercise, 'around the world') && hasPrimary(exercise, 'shoulders')) return ['front delts', 'side delts'];

  // Arnold press = all three heads
  if (nameHas(exercise, 'arnold')) return ['front delts', 'side delts'];

  // Overhead/military press = front + side delts
  if (nameHas(exercise, 'overhead press', 'military press', 'shoulder press', 'standing press', 'log press', 'push press', 'clean and press', 'bradford', 'savickas')) {
    return ['front delts', 'side delts'];
  }
  if (nameHas(exercise, 'press') && hasPrimary(exercise, 'shoulders') && !nameHas(exercise, 'bench')) {
    return ['front delts', 'side delts'];
  }

  // Shrug-like for shoulders
  if (nameHas(exercise, 'shrug')) return ['upper traps'];

  // External/internal rotation
  if (nameHas(exercise, 'external rotation', 'rotator')) return ['rear delts', 'rotator cuff'];
  if (nameHas(exercise, 'internal rotation')) return ['front delts', 'rotator cuff'];
  if (nameHas(exercise, 'cuban')) return ['rear delts', 'rotator cuff'];

  // Cable/machine raises
  if (nameHas(exercise, 'cable') && nameHas(exercise, 'raise')) {
    if (nameHas(exercise, 'front')) return ['front delts'];
    if (nameHas(exercise, 'lateral', 'side')) return ['side delts'];
    return ['side delts'];
  }

  // Arm circles, scaption
  if (nameHas(exercise, 'circle', 'scaption')) return ['front delts', 'side delts'];

  // Medicine ball throw
  if (nameHas(exercise, 'throw', 'toss', 'slam')) return ['front delts'];

  // Handstand
  if (nameHas(exercise, 'handstand')) return ['front delts', 'side delts'];

  // Anti-gravity press
  if (nameHas(exercise, 'anti-gravity')) return ['rear delts'];

  // Default for generic shoulder exercises
  return ['front delts', 'side delts'];
}

function getBicepsSubTargets(exercise) {
  const nm = n(exercise);

  // Hammer curl = brachialis + long head
  if (nameHas(exercise, 'hammer', 'cross-body', 'cross body', 'pinwheel')) return ['long head', 'brachialis'];
  // Reverse curl = brachialis + brachioradialis
  if (nameHas(exercise, 'reverse curl', 'reverse grip curl', 'reverse barbell curl')) return ['brachialis', 'brachioradialis'];
  // Preacher/concentration = short head
  if (nameHas(exercise, 'preacher', 'concentration', 'scott')) return ['short head'];
  // Spider curl = short head
  if (nameHas(exercise, 'spider')) return ['short head'];
  // Incline curl = long head (stretched)
  if (nameHas(exercise, 'incline') && nameHas(exercise, 'curl')) return ['long head'];
  // Drag curl = long head
  if (nameHas(exercise, 'drag')) return ['long head'];
  // Cable curl behind = long head
  if (nameHas(exercise, 'overhead cable curl', 'high cable curl', 'lying cable curl')) return ['long head'];
  // Wide grip = short head
  if (nameHas(exercise, 'wide') && nameHas(exercise, 'curl')) return ['short head'];
  // Close/narrow grip = long head
  if (nameHas(exercise, 'close', 'narrow') && nameHas(exercise, 'curl')) return ['long head'];
  // EZ bar/barbell curl = both heads
  if (nameHas(exercise, 'barbell curl', 'ez-bar', 'ez bar', 'standing curl')) return ['long head', 'short head'];
  // Zottman = both heads + brachialis
  if (nameHas(exercise, 'zottman')) return ['long head', 'short head', 'brachialis'];
  // Dumbbell curl general
  if (nameHas(exercise, 'dumbbell curl', 'cable curl', 'machine curl')) return ['long head', 'short head'];
  // Chin-up with bicep focus
  if (nameHas(exercise, 'chin-up', 'chin up', 'chinup')) return ['short head'];
  // Default
  return ['long head', 'short head'];
}

function getTricepsSubTargets(exercise) {
  const nm = n(exercise);

  // Overhead extension = long head
  if (nameHas(exercise, 'overhead', 'french press', 'behind', 'skullcrusher', 'skull crusher', 'lying tricep', 'lying extension')) return ['long head'];
  // Kickback = lateral + long head
  if (nameHas(exercise, 'kickback', 'kick back', 'kick-back')) return ['long head', 'lateral head'];
  // Pushdown/pressdown = lateral head primarily
  if (nameHas(exercise, 'pushdown', 'push down', 'press down', 'pressdown')) {
    if (nameHas(exercise, 'reverse', 'underhand')) return ['medial head'];
    if (nameHas(exercise, 'rope')) return ['lateral head', 'long head'];
    return ['lateral head'];
  }
  // Close grip bench = all heads, medial emphasis
  if (nameHas(exercise, 'close grip', 'close-grip', 'narrow grip')) return ['medial head', 'lateral head'];
  // Dips for triceps
  if (nameHas(exercise, 'dip', 'bench dip')) return ['lateral head', 'medial head'];
  // Diamond push-ups
  if (nameHas(exercise, 'diamond')) return ['lateral head', 'medial head'];
  // Band skull crusher
  if (nameHas(exercise, 'band skull')) return ['long head'];
  // Tate press
  if (nameHas(exercise, 'tate')) return ['lateral head', 'medial head'];
  // JM press
  if (nameHas(exercise, 'jm press')) return ['medial head', 'lateral head'];
  // Default
  return ['lateral head', 'long head'];
}

function getQuadricepsSubTargets(exercise) {
  const nm = n(exercise);

  // Leg extension = rectus femoris + VMO
  if (nameHas(exercise, 'leg extension', 'leg ext')) return ['rectus femoris', 'vastus medialis'];
  // Sissy squat = rectus femoris
  if (nameHas(exercise, 'sissy')) return ['rectus femoris'];
  // Hack squat = vastus lateralis + VMO
  if (nameHas(exercise, 'hack squat', 'hack machine')) return ['vastus lateralis', 'vastus medialis'];
  // Front squat = vastus medialis + rectus femoris
  if (nameHas(exercise, 'front squat', 'zercher', 'goblet')) return ['vastus medialis', 'rectus femoris'];
  // Close/narrow stance = VMO emphasis
  if (nameHas(exercise, 'close stance', 'narrow stance', 'narrow squat', 'close squat')) return ['vastus medialis'];
  // Wide stance = vastus lateralis + adductors
  if (nameHas(exercise, 'wide stance', 'sumo') && hasPrimary(exercise, 'quadriceps')) return ['vastus lateralis'];
  // Bulgarian/split squat = all quad heads
  if (nameHas(exercise, 'bulgarian', 'split squat')) return ['vastus medialis', 'vastus lateralis', 'rectus femoris'];
  // Lunge variations
  if (nameHas(exercise, 'lunge')) {
    if (nameHas(exercise, 'walking', 'forward')) return ['vastus lateralis', 'rectus femoris'];
    if (nameHas(exercise, 'reverse', 'backward')) return ['vastus medialis', 'rectus femoris'];
    if (nameHas(exercise, 'lateral', 'side')) return ['vastus lateralis'];
    return ['vastus lateralis', 'rectus femoris'];
  }
  // Step-up
  if (nameHas(exercise, 'step-up', 'step up', 'box step')) return ['vastus medialis', 'rectus femoris'];
  // Leg press
  if (nameHas(exercise, 'leg press')) return ['vastus lateralis', 'vastus medialis'];
  // Squat variations (general)
  if (nameHas(exercise, 'squat')) return ['vastus lateralis', 'vastus medialis', 'rectus femoris'];
  // Wall sit
  if (nameHas(exercise, 'wall sit', 'wall squat')) return ['vastus medialis', 'rectus femoris'];
  // Sprint/run/jump
  if (nameHas(exercise, 'sprint', 'run', 'jump', 'bound', 'box jump', 'tuck jump')) return ['rectus femoris', 'vastus lateralis'];
  // Thrusters
  if (nameHas(exercise, 'thruster')) return ['vastus lateralis', 'vastus medialis', 'rectus femoris'];
  // Sled
  if (nameHas(exercise, 'sled', 'prowler', 'drag')) return ['vastus lateralis', 'rectus femoris'];
  // Stretch
  if (nameHas(exercise, 'stretch', 'smr', 'foam roll')) return ['rectus femoris', 'vastus lateralis'];
  // Clean/snatch
  if (nameHas(exercise, 'clean', 'snatch')) return ['vastus lateralis', 'rectus femoris'];
  // Default
  return ['vastus lateralis', 'vastus medialis', 'rectus femoris'];
}

function getHamstringsSubTargets(exercise) {
  const nm = n(exercise);

  // Leg curl = biceps femoris emphasis
  if (nameHas(exercise, 'leg curl', 'lying curl', 'seated curl')) return ['biceps femoris', 'semitendinosus'];
  // Romanian/stiff-leg deadlift = semimembranosus/semitendinosus
  if (nameHas(exercise, 'romanian', 'stiff', 'straight leg', 'straight-leg')) return ['semitendinosus', 'semimembranosus'];
  // Good morning
  if (nameHas(exercise, 'good morning')) return ['semitendinosus', 'semimembranosus'];
  // Nordic curl = all hamstrings
  if (nameHas(exercise, 'nordic', 'natural glute ham', 'glute ham', 'glute-ham')) return ['biceps femoris', 'semitendinosus', 'semimembranosus'];
  // Pull through
  if (nameHas(exercise, 'pull through', 'pull-through')) return ['semitendinosus', 'semimembranosus'];
  // Ball leg curl
  if (nameHas(exercise, 'ball leg curl', 'stability ball')) return ['biceps femoris', 'semitendinosus'];
  // Deadlift (conventional)
  if (nameHas(exercise, 'deadlift')) return ['biceps femoris', 'semitendinosus'];
  // Kettlebell swing
  if (nameHas(exercise, 'swing')) return ['biceps femoris'];
  // Clean/snatch
  if (nameHas(exercise, 'clean', 'snatch', 'hang')) return ['biceps femoris'];
  // Stretch
  if (nameHas(exercise, 'stretch', 'smr', 'foam roll', '90/90')) return ['biceps femoris', 'semitendinosus'];
  // Default
  return ['biceps femoris', 'semitendinosus'];
}

function getGlutesSubTargets(exercise) {
  const nm = n(exercise);

  // Abduction/lateral movements = gluteus medius
  if (nameHas(exercise, 'abduct', 'lateral', 'side lying', 'side-lying', 'clamshell', 'clam shell', 'fire hydrant', 'band walk')) return ['gluteus medius', 'gluteus minimus'];
  // Hip thrust, bridge = gluteus maximus
  if (nameHas(exercise, 'hip thrust', 'glute bridge', 'bridge', 'thrust')) return ['gluteus maximus'];
  // Kickback = gluteus maximus
  if (nameHas(exercise, 'kickback', 'kick back', 'donkey kick', 'cable kick')) return ['gluteus maximus'];
  // Single leg = medius + maximus
  if (nameHas(exercise, 'single leg', 'single-leg', 'one leg', 'pistol')) return ['gluteus maximus', 'gluteus medius'];
  // Step-up
  if (nameHas(exercise, 'step-up', 'step up')) return ['gluteus maximus', 'gluteus medius'];
  // Squat/lunge
  if (nameHas(exercise, 'squat', 'lunge')) return ['gluteus maximus'];
  // Hip extension
  if (nameHas(exercise, 'hip extension', 'pull-through', 'pull through')) return ['gluteus maximus'];
  // Stretch/mobility
  if (nameHas(exercise, 'stretch', 'pigeon', 'ankle on the knee', 'seated hip', 'smr')) return ['gluteus maximus', 'gluteus medius'];
  // Default
  return ['gluteus maximus'];
}

function getCalvesSubTargets(exercise) {
  const nm = n(exercise);

  // Seated calf raise = soleus
  if (nameHas(exercise, 'seated calf', 'seated')) return ['soleus'];
  // Standing calf raise = gastrocnemius
  if (nameHas(exercise, 'standing calf', 'donkey calf', 'calf press', 'calf raise') && !nameHas(exercise, 'seated')) return ['gastrocnemius'];
  // Smith machine calf raise
  if (nameHas(exercise, 'smith') && nameHas(exercise, 'calf')) return ['gastrocnemius'];
  // Ankle/tibialis
  if (nameHas(exercise, 'tibialis', 'toe raise', 'ankle dorsiflexion')) return ['tibialis anterior'];
  // Ankle circles
  if (nameHas(exercise, 'ankle circle')) return ['gastrocnemius', 'soleus'];
  // Jump rope
  if (nameHas(exercise, 'jump rope', 'skipping')) return ['gastrocnemius'];
  // SMR/foam roll
  if (nameHas(exercise, 'smr', 'foam roll')) return ['gastrocnemius', 'soleus'];
  // Balance board
  if (nameHas(exercise, 'balance')) return ['gastrocnemius', 'soleus'];
  // Default
  return ['gastrocnemius', 'soleus'];
}

function getAbdominalsSubTargets(exercise) {
  const nm = n(exercise);

  // Lower abs - leg raises, reverse crunch, hip raise
  if (nameHas(exercise, 'leg raise', 'knee raise', 'hip raise', 'reverse crunch', 'hanging', 'captain', 'toes to bar', 'toes-to-bar', 'l-sit', 'flutter kick', 'scissor', 'bottoms up', 'butt-up', 'flat bench lying')) return ['lower abs'];
  // Obliques - twists, side bends, rotations, woodchop
  if (nameHas(exercise, 'oblique', 'twist', 'russian', 'side bend', 'side crunch', 'woodchop', 'wood chop', 'windshield', 'rotation', 'rotary', 'windmill', 'judo flip')) return ['obliques'];
  // Cross-body = obliques
  if (nameHas(exercise, 'cross-body', 'cross body', 'bicycle', 'air bike', 'elbow-to-knee', 'heel touch', 'alternate heel')) return ['obliques', 'upper abs'];
  // Plank/vacuum = transverse abdominis
  if (nameHas(exercise, 'plank', 'vacuum', 'stomach vacuum', 'hollow', 'dead bug')) return ['transverse abdominis'];
  // Ab roller/rollout = upper abs + transverse
  if (nameHas(exercise, 'roller', 'rollout', 'roll out', 'ab roll', 'ab wheel')) return ['upper abs', 'transverse abdominis'];
  // Crunch/sit-up = upper abs
  if (nameHas(exercise, 'crunch', 'sit-up', 'sit up', 'situp')) {
    if (nameHas(exercise, 'reverse')) return ['lower abs'];
    if (nameHas(exercise, 'side', 'oblique', 'cross')) return ['obliques'];
    if (nameHas(exercise, 'cable')) return ['upper abs'];
    return ['upper abs'];
  }
  // V-up, jackknife = upper + lower
  if (nameHas(exercise, 'v-up', 'jackknife', 'jack knife', 'tuck', 'dragon flag')) return ['upper abs', 'lower abs'];
  // Mountain climber
  if (nameHas(exercise, 'mountain climber')) return ['lower abs', 'obliques'];
  // Pallof press = transverse + obliques
  if (nameHas(exercise, 'pallof', 'anti-rotation')) return ['transverse abdominis', 'obliques'];
  // Landmine
  if (nameHas(exercise, 'landmine') && hasPrimary(exercise, 'abdominals')) return ['obliques'];
  // Cable/weighted exercises
  if (nameHas(exercise, 'cable crunch', 'weighted crunch', 'machine crunch')) return ['upper abs'];
  // Spell caster
  if (nameHas(exercise, 'spell caster')) return ['obliques'];
  // Default
  return ['upper abs'];
}

function getLatsSubTargets(exercise) {
  const nm = n(exercise);

  // Wide grip = upper/outer lats
  if (nameHas(exercise, 'wide grip', 'wide-grip')) return ['upper lats'];
  // Close/narrow grip = lower lats
  if (nameHas(exercise, 'close grip', 'close-grip', 'narrow grip', 'narrow-grip', 'v-bar', 'v bar')) return ['lower lats'];
  // Pull-up/chin-up
  if (nameHas(exercise, 'pull-up', 'pullup', 'pull up')) {
    if (nameHas(exercise, 'wide')) return ['upper lats'];
    if (nameHas(exercise, 'close', 'narrow')) return ['lower lats'];
    return ['upper lats', 'lower lats'];
  }
  if (nameHas(exercise, 'chin-up', 'chinup', 'chin up')) return ['lower lats'];
  // Pulldown
  if (nameHas(exercise, 'pulldown', 'pull-down', 'pull down', 'lat pull')) {
    if (nameHas(exercise, 'wide')) return ['upper lats'];
    if (nameHas(exercise, 'close', 'narrow', 'v-bar')) return ['lower lats'];
    if (nameHas(exercise, 'behind', 'back')) return ['upper lats'];
    if (nameHas(exercise, 'reverse', 'underhand', 'supinated')) return ['lower lats'];
    return ['upper lats', 'lower lats'];
  }
  // Straight arm pulldown / pullover = lower lats
  if (nameHas(exercise, 'straight arm', 'straight-arm', 'pullover', 'pull over')) return ['lower lats'];
  // Row
  if (nameHas(exercise, 'row')) {
    if (nameHas(exercise, 'underhand', 'supinated', 'reverse')) return ['lower lats'];
    return ['upper lats', 'lower lats'];
  }
  // Muscle up
  if (nameHas(exercise, 'muscle up', 'muscle-up')) return ['upper lats', 'lower lats'];
  // SMR
  if (nameHas(exercise, 'smr', 'foam')) return ['upper lats', 'lower lats'];
  // Default
  return ['upper lats', 'lower lats'];
}

function getMiddleBackSubTargets(exercise) {
  const nm = n(exercise);

  // Rows = rhomboids + mid traps
  if (nameHas(exercise, 'row')) {
    if (nameHas(exercise, 'close', 'narrow')) return ['rhomboids'];
    if (nameHas(exercise, 'wide')) return ['mid traps', 'rhomboids'];
    if (nameHas(exercise, 'seated cable')) return ['rhomboids', 'mid traps'];
    return ['rhomboids', 'mid traps'];
  }
  // Face pull
  if (nameHas(exercise, 'face pull')) return ['mid traps', 'rhomboids'];
  // Band pull apart
  if (nameHas(exercise, 'band pull apart')) return ['rhomboids', 'mid traps'];
  // T-bar
  if (nameHas(exercise, 't-bar', 'tbar')) return ['rhomboids', 'mid traps'];
  // Reverse fly
  if (nameHas(exercise, 'reverse fly', 'reverse flye', 'rear delt')) return ['rhomboids'];
  // Hyperextension/back extension when middle back
  if (nameHas(exercise, 'hyperextension', 'back extension')) return ['erector spinae'];
  // Superman
  if (nameHas(exercise, 'superman', 'super man')) return ['rhomboids', 'erector spinae'];
  // Shrug
  if (nameHas(exercise, 'shrug')) return ['mid traps'];
  // Default
  return ['rhomboids', 'mid traps'];
}

function getTrapsSubTargets(exercise) {
  const nm = n(exercise);

  // Shrugs = upper traps
  if (nameHas(exercise, 'shrug')) return ['upper traps'];
  // Upright row = upper traps
  if (nameHas(exercise, 'upright row')) return ['upper traps'];
  // Face pull = lower/mid traps
  if (nameHas(exercise, 'face pull')) return ['mid traps', 'lower traps'];
  // Y-raise = lower traps
  if (nameHas(exercise, 'y-raise', 'y raise', 'prone y')) return ['lower traps'];
  // Scapular retraction/depression = lower + mid traps
  if (nameHas(exercise, 'scapular', 'scap')) return ['lower traps', 'mid traps'];
  // Farmer walk/carry
  if (nameHas(exercise, 'farmer', 'carry', 'walk') && has(exercise, 'traps')) return ['upper traps'];
  // Rack pull/deadlift
  if (nameHas(exercise, 'rack pull', 'deadlift')) return ['upper traps'];
  // Clean/snatch
  if (nameHas(exercise, 'clean', 'snatch', 'high pull')) return ['upper traps'];
  // Default
  return ['upper traps'];
}

function getLowerBackSubTargets(exercise) {
  const nm = n(exercise);

  // Back extension/hyperextension
  if (nameHas(exercise, 'hyperextension', 'back extension', 'reverse hyper')) return ['erector spinae'];
  // Good morning
  if (nameHas(exercise, 'good morning')) return ['erector spinae'];
  // Deadlift
  if (nameHas(exercise, 'deadlift', 'dead lift')) return ['erector spinae'];
  // Superman
  if (nameHas(exercise, 'superman')) return ['erector spinae'];
  // Atlas stones / strongman
  if (nameHas(exercise, 'atlas', 'stone')) return ['erector spinae'];
  // SMR
  if (nameHas(exercise, 'smr', 'foam roll')) return ['erector spinae'];
  // Rack pull
  if (nameHas(exercise, 'rack pull')) return ['erector spinae'];
  // Default
  return ['erector spinae'];
}

function getForearmSubTargets(exercise) {
  const nm = n(exercise);

  // Wrist curl = wrist flexors
  if (nameHas(exercise, 'wrist curl') && !nameHas(exercise, 'reverse')) return ['wrist flexors'];
  // Reverse wrist curl = wrist extensors
  if (nameHas(exercise, 'reverse wrist', 'wrist extension')) return ['wrist extensors'];
  // Hammer/reverse curl = brachioradialis
  if (nameHas(exercise, 'hammer', 'reverse curl')) return ['brachioradialis'];
  // Grip/farmer = wrist flexors + brachioradialis
  if (nameHas(exercise, 'grip', 'farmer', 'plate pinch', 'finger', 'hand grip')) return ['wrist flexors', 'brachioradialis'];
  // Wrist roller
  if (nameHas(exercise, 'wrist roller', 'roller')) return ['wrist flexors', 'wrist extensors'];
  // Palms up/down
  if (nameHas(exercise, 'palms-up', 'palms up')) return ['wrist flexors'];
  if (nameHas(exercise, 'palms-down', 'palms down')) return ['wrist extensors'];
  // Spinner
  if (nameHas(exercise, 'spinner')) return ['wrist flexors', 'wrist extensors'];
  // Default
  return ['wrist flexors', 'brachioradialis'];
}

function getAdductorsSubTargets(exercise) {
  const nm = n(exercise);

  if (nameHas(exercise, 'groin')) return ['adductor longus', 'gracilis'];
  if (nameHas(exercise, 'sumo')) return ['adductor magnus', 'adductor longus'];
  if (nameHas(exercise, 'copenhagen')) return ['adductor longus'];
  // Default
  return ['adductor magnus', 'adductor longus'];
}

function getAbductorsSubTargets(exercise) {
  const nm = n(exercise);

  if (nameHas(exercise, 'clam', 'fire hydrant')) return ['gluteus medius', 'gluteus minimus'];
  if (nameHas(exercise, 'band walk', 'lateral walk', 'monster walk')) return ['gluteus medius'];
  // Default
  return ['gluteus medius', 'tensor fasciae latae'];
}

function getNeckSubTargets(exercise) {
  const nm = n(exercise);

  if (nameHas(exercise, 'flexion', 'front')) return ['sternocleidomastoid'];
  if (nameHas(exercise, 'extension', 'back', 'rear')) return ['upper traps', 'splenius'];
  if (nameHas(exercise, 'lateral', 'side')) return ['sternocleidomastoid', 'scalenes'];
  if (nameHas(exercise, 'rotation')) return ['sternocleidomastoid'];
  // Default
  return ['sternocleidomastoid', 'upper traps'];
}

// ==================== MAIN MAPPING ====================

const subTargetFunctions = {
  'chest': getChestSubTargets,
  'shoulders': getShoulderSubTargets,
  'biceps': getBicepsSubTargets,
  'triceps': getTricepsSubTargets,
  'quadriceps': getQuadricepsSubTargets,
  'hamstrings': getHamstringsSubTargets,
  'glutes': getGlutesSubTargets,
  'calves': getCalvesSubTargets,
  'abdominals': getAbdominalsSubTargets,
  'lats': getLatsSubTargets,
  'middle back': getMiddleBackSubTargets,
  'traps': getTrapsSubTargets,
  'lower back': getLowerBackSubTargets,
  'forearms': getForearmSubTargets,
  'adductors': getAdductorsSubTargets,
  'abductors': getAbductorsSubTargets,
  'neck': getNeckSubTargets,
};

// ==================== SECONDARY SUB-TARGETS ====================
// For secondary muscles, we use simpler/contextual mappings

function getSecondarySubTarget(exercise, muscle) {
  const nm = n(exercise);

  switch (muscle) {
    case 'chest':
      if (nameHas(exercise, 'incline')) return ['upper chest'];
      if (nameHas(exercise, 'decline')) return ['lower chest'];
      return ['mid chest'];

    case 'shoulders':
      // When shoulders are secondary, determine which head is engaged
      if (nameHas(exercise, 'bench press', 'chest press', 'push-up', 'pushup', 'push up', 'dip', 'floor press')) return ['front delts'];
      if (nameHas(exercise, 'row', 'pull', 'face pull', 'reverse fly')) return ['rear delts'];
      if (nameHas(exercise, 'lateral', 'upright')) return ['side delts'];
      if (nameHas(exercise, 'overhead', 'press', 'clean', 'snatch', 'jerk')) return ['front delts'];
      if (nameHas(exercise, 'roller', 'rollout', 'ab roll', 'plank')) return ['front delts'];
      if (nameHas(exercise, 'deadlift', 'shrug')) return ['front delts'];
      return ['front delts'];

    case 'triceps':
      // Triceps secondary = usually just general engagement
      if (nameHas(exercise, 'close grip', 'narrow')) return ['medial head', 'lateral head'];
      if (nameHas(exercise, 'overhead', 'press')) return ['lateral head'];
      return ['lateral head'];

    case 'biceps':
      if (nameHas(exercise, 'row', 'pull', 'curl')) return ['short head'];
      if (nameHas(exercise, 'hammer', 'neutral')) return ['brachialis'];
      return ['short head'];

    case 'forearms':
      if (nameHas(exercise, 'curl', 'pull', 'row', 'deadlift')) return ['brachioradialis'];
      if (nameHas(exercise, 'hammer')) return ['brachioradialis'];
      return ['wrist flexors'];

    case 'quadriceps':
      return ['vastus lateralis', 'rectus femoris'];

    case 'hamstrings':
      return ['biceps femoris'];

    case 'glutes':
      if (nameHas(exercise, 'lateral', 'abduct', 'side')) return ['gluteus medius'];
      return ['gluteus maximus'];

    case 'calves':
      return ['gastrocnemius'];

    case 'abdominals':
      if (nameHas(exercise, 'twist', 'rotation', 'side')) return ['obliques'];
      if (nameHas(exercise, 'plank', 'hold')) return ['transverse abdominis'];
      return ['upper abs'];

    case 'lats':
      return ['upper lats'];

    case 'middle back':
      return ['rhomboids'];

    case 'traps':
      if (nameHas(exercise, 'deadlift', 'shrug', 'clean', 'carry', 'farmer')) return ['upper traps'];
      if (nameHas(exercise, 'row', 'pull')) return ['mid traps'];
      return ['upper traps'];

    case 'lower back':
      return ['erector spinae'];

    case 'adductors':
      return ['adductor longus'];

    case 'abductors':
      return ['gluteus medius'];

    case 'neck':
      return ['sternocleidomastoid'];

    default:
      return [];
  }
}

// ==================== PROCESS ALL EXERCISES ====================

let noSubTargetCount = 0;

data.forEach(exercise => {
  // Primary sub-targets
  const primarySubs = [];
  exercise.primaryMuscles.forEach(muscle => {
    const fn = subTargetFunctions[muscle];
    if (fn) {
      const subs = fn(exercise);
      subs.forEach(s => { if (!primarySubs.includes(s)) primarySubs.push(s); });
    }
  });
  exercise.primarySubTargets = primarySubs;

  // Secondary sub-targets
  const secondarySubs = [];
  (exercise.secondaryMuscles || []).forEach(muscle => {
    const subs = getSecondarySubTarget(exercise, muscle);
    subs.forEach(s => { if (!secondarySubs.includes(s)) secondarySubs.push(s); });
  });
  exercise.secondarySubTargets = secondarySubs;

  if (primarySubs.length === 0) {
    noSubTargetCount++;
    console.log(`NO PRIMARY SUB-TARGET: ${exercise.name} (primary: ${exercise.primaryMuscles.join(', ')})`);
  }
});

// Write output
fs.writeFileSync('app/src/main/res/raw/exercises.json', JSON.stringify(data, null, 2), 'utf8');

console.log(`\nProcessed ${data.length} exercises`);
console.log(`Exercises without primary sub-targets: ${noSubTargetCount}`);

// Stats
const allPrimarySubs = new Set();
const allSecondarySubs = new Set();
data.forEach(e => {
  e.primarySubTargets.forEach(s => allPrimarySubs.add(s));
  e.secondarySubTargets.forEach(s => allSecondarySubs.add(s));
});
console.log(`\nUnique primary sub-targets: ${[...allPrimarySubs].sort().join(', ')}`);
console.log(`\nUnique secondary sub-targets: ${[...allSecondarySubs].sort().join(', ')}`);

// Sample output
console.log('\n--- SAMPLE EXERCISES ---');
['Barbell Bench Press - Medium Grip', 'Barbell Incline Bench Press - Medium Grip', 'Dumbbell Flyes', 'Arnold Dumbbell Press', 'Barbell Curl', 'Hammer Curls', 'Preacher Curl', 'Skull Crusher', 'Barbell Full Squat', 'Romanian Deadlift With Dumbbells', 'Standing Calf Raises', 'Ab Roller', 'Pull-ups', 'Barbell Deadlift', 'Barbell Shrug'].forEach(name => {
  const ex = data.find(e => e.name === name);
  if (ex) {
    console.log(`\n${ex.name}`);
    console.log(`  Primary: ${ex.primaryMuscles.join(', ')} => Sub: ${ex.primarySubTargets.join(', ')}`);
    console.log(`  Secondary: ${(ex.secondaryMuscles||[]).join(', ')} => Sub: ${ex.secondarySubTargets.join(', ')}`);
  }
});
