const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'app/src/main/res/raw/exercises.json');
const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));

// Build name->exercise lookup
const byName = {};
data.forEach(ex => { byName[ex.name.toLowerCase()] = ex; });

// ============================================================
// SUB-MUSCLE CLASSIFICATION
// We classify each exercise into a fine-grained sub-target
// based on name keywords, primary/secondary muscles, equipment
// ============================================================

function classifySubTarget(ex) {
  const n = ex.name.toLowerCase();
  const pm = (ex.primaryMuscles || []).map(m => m.toLowerCase());
  const sm = (ex.secondaryMuscles || []).map(m => m.toLowerCase());
  const eq = (ex.equipment || '').toLowerCase();
  const cat = (ex.category || '').toLowerCase();
  const force = (ex.force || '').toLowerCase();

  // === CHEST ===
  if (pm.includes('chest')) {
    if (n.includes('incline')) return 'upper_chest';
    if (n.includes('decline')) return 'lower_chest';
    if (n.includes('fly') || n.includes('flye') || n.includes('crossover') || n.includes('cross-over') || n.includes('cable fly') || n.includes('pec deck') || n.includes('butterfly') || n.includes('around the world') || n.includes('svend')) return 'inner_chest';
    if (n.includes('pullover')) return 'chest_pullover';
    if (n.includes('push-up') || n.includes('pushup') || n.includes('push up')) {
      if (n.includes('decline')) return 'upper_chest';
      if (n.includes('incline')) return 'lower_chest';
      if (n.includes('wide') || n.includes('diamond')) return 'inner_chest';
      return 'mid_chest';
    }
    if (n.includes('dip')) return 'lower_chest';
    if (n.includes('floor press')) return 'mid_chest';
    return 'mid_chest';
  }

  // === SHOULDERS ===
  if (pm.includes('shoulders')) {
    if (n.includes('lateral') || n.includes('side raise') || n.includes('side lying') || n.includes('leaning') || n.includes('car driver') || n.includes('iron cross') || n.includes('cuban')) return 'lateral_delt';
    if (n.includes('front raise') || n.includes('front delt') || n.includes('plate raise') || n.includes('front plate') || n.includes('bus driver')) return 'front_delt';
    if (n.includes('rear') || n.includes('reverse fly') || n.includes('reverse flye') || n.includes('face pull') || n.includes('bent-over') || n.includes('bent over') || n.includes('posterior') || n.includes('lying rear')) return 'rear_delt';
    if (n.includes('press') || n.includes('push press') || n.includes('arnold') || n.includes('military') || n.includes('overhead') || n.includes('clean and press') || n.includes('log lift') || n.includes('bradford') || n.includes('anti-gravity') || n.includes('handstand')) return 'overhead_press';
    if (n.includes('upright row')) return 'lateral_delt';
    if (n.includes('shrug')) return 'traps_upper';
    if (n.includes('circle') || n.includes('rotation') || n.includes('rotator') || n.includes('internal') || n.includes('external')) return 'rotator_cuff';
    if (n.includes('raise')) {
      if (n.includes('front')) return 'front_delt';
      if (n.includes('rear') || n.includes('back')) return 'rear_delt';
      return 'lateral_delt';
    }
    if (cat === 'stretching') return 'shoulder_stretch';
    return 'overhead_press';
  }

  // === BACK - LATS ===
  if (pm.includes('lats')) {
    if (n.includes('pulldown') || n.includes('pull-down') || n.includes('pullup') || n.includes('pull-up') || n.includes('chin-up') || n.includes('chinup') || n.includes('chin up') || n.includes('pull up')) {
      if (n.includes('close') || n.includes('narrow') || n.includes('v-bar') || n.includes('underhand') || n.includes('reverse') || n.includes('chin')) return 'lats_lower';
      if (n.includes('wide') || n.includes('behind')) return 'lats_width';
      return 'lats_width';
    }
    if (n.includes('row')) return 'lats_thickness';
    if (n.includes('pullover') || n.includes('straight-arm') || n.includes('straight arm')) return 'lats_stretch';
    return 'lats_width';
  }

  // === MIDDLE BACK ===
  if (pm.includes('middle back')) {
    if (n.includes('row')) {
      if (n.includes('cable') || n.includes('seated') || n.includes('machine')) return 'mid_back_row';
      if (n.includes('bent') || n.includes('barbell') || n.includes('dumbbell') || n.includes('t-bar') || n.includes('pendlay') || n.includes('meadows') || n.includes('landmine')) return 'mid_back_row';
      if (n.includes('inverted') || n.includes('body')) return 'mid_back_bodyweight';
      return 'mid_back_row';
    }
    if (n.includes('pull') || n.includes('pulldown')) return 'mid_back_pull';
    if (n.includes('reverse') || n.includes('rear') || n.includes('face pull')) return 'rear_delt';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr')) return 'mid_back_stretch';
    return 'mid_back_row';
  }

  // === LOWER BACK ===
  if (pm.includes('lower back')) {
    if (n.includes('deadlift') || n.includes('dead lift')) return 'lower_back_deadlift';
    if (n.includes('hyperextension') || n.includes('back extension') || n.includes('superman') || n.includes('reverse hyper')) return 'lower_back_extension';
    if (n.includes('good morning') || n.includes('goodmorning')) return 'lower_back_hinge';
    if (n.includes('stretch') || n.includes('foam') || n.includes('smr') || cat === 'stretching') return 'lower_back_stretch';
    if (n.includes('swing') || n.includes('clean') || n.includes('snatch')) return 'lower_back_power';
    return 'lower_back_extension';
  }

  // === TRAPS ===
  if (pm.includes('traps')) {
    if (n.includes('shrug')) return 'traps_upper';
    if (n.includes('row') || n.includes('upright')) return 'traps_mid';
    if (n.includes('face pull') || n.includes('reverse') || n.includes('rear')) return 'traps_lower';
    if (n.includes('clean') || n.includes('snatch') || n.includes('hang') || n.includes('power')) return 'traps_power';
    if (n.includes('farmer') || n.includes('carry')) return 'traps_upper';
    return 'traps_upper';
  }

  // === BICEPS ===
  if (pm.includes('biceps')) {
    if (n.includes('hammer') || n.includes('neutral') || n.includes('cross body') || n.includes('crossbody') || n.includes('pinwheel')) return 'biceps_brachialis';
    if (n.includes('preacher') || n.includes('concentration') || n.includes('spider') || n.includes('scott')) return 'biceps_short_head';
    if (n.includes('incline') || n.includes('drag') || n.includes('bayesian')) return 'biceps_long_head';
    if (n.includes('reverse') || n.includes('zottman') || n.includes('pronated')) return 'biceps_brachioradialis';
    if (n.includes('cable') || n.includes('machine')) return 'biceps_general';
    if (n.includes('21') || n.includes('twenty')) return 'biceps_general';
    if (n.includes('curl')) return 'biceps_general';
    if (cat === 'stretching' || n.includes('stretch')) return 'biceps_stretch';
    return 'biceps_general';
  }

  // === TRICEPS ===
  if (pm.includes('triceps')) {
    if (n.includes('overhead') || n.includes('french') || n.includes('skullcrusher') || n.includes('skull') || n.includes('lying') || n.includes('incline') || n.includes('overhead extension')) return 'triceps_long_head';
    if (n.includes('pushdown') || n.includes('press-down') || n.includes('push-down') || n.includes('cable') || n.includes('rope') || n.includes('v-bar') || n.includes('reverse grip pushdown')) return 'triceps_lateral_head';
    if (n.includes('kickback') || n.includes('kick back') || n.includes('kick-back')) return 'triceps_lateral_head';
    if (n.includes('close grip') || n.includes('close-grip') || n.includes('narrow') || n.includes('diamond') || n.includes('board press')) return 'triceps_medial_head';
    if (n.includes('dip') || n.includes('bench dip')) return 'triceps_general';
    if (n.includes('press') || n.includes('extension')) return 'triceps_long_head';
    if (cat === 'stretching' || n.includes('stretch')) return 'triceps_stretch';
    return 'triceps_general';
  }

  // === FOREARMS ===
  if (pm.includes('forearms')) {
    if (n.includes('wrist curl') || n.includes('wrist roller') || n.includes('palms-up') || n.includes('palms up')) return 'forearms_flexors';
    if (n.includes('reverse') || n.includes('palms-down') || n.includes('palms down') || n.includes('wrist extension')) return 'forearms_extensors';
    if (n.includes('grip') || n.includes('farmer') || n.includes('carry') || n.includes('crush') || n.includes('pinch') || n.includes('towel') || n.includes('finger')) return 'forearms_grip';
    if (n.includes('pronation') || n.includes('supination') || n.includes('rotation')) return 'forearms_rotation';
    if (cat === 'stretching' || n.includes('stretch')) return 'forearms_stretch';
    return 'forearms_flexors';
  }

  // === QUADRICEPS ===
  if (pm.includes('quadriceps')) {
    if (n.includes('squat') || n.includes('squats')) {
      if (n.includes('front') || n.includes('goblet') || n.includes('zercher') || n.includes('hack')) return 'quads_front_squat';
      if (n.includes('sissy')) return 'quads_rectus_femoris';
      if (n.includes('split') || n.includes('bulgarian') || n.includes('lunge')) return 'quads_unilateral';
      if (n.includes('sumo') || n.includes('wide')) return 'quads_inner';
      return 'quads_squat';
    }
    if (n.includes('lunge') || n.includes('step-up') || n.includes('step up') || n.includes('split') || n.includes('pistol') || n.includes('single leg') || n.includes('single-leg') || n.includes('one leg') || n.includes('one-leg')) return 'quads_unilateral';
    if (n.includes('leg press') || n.includes('press')) return 'quads_squat';
    if (n.includes('extension') || n.includes('leg extension')) return 'quads_rectus_femoris';
    if (n.includes('jump') || n.includes('box jump') || n.includes('bound') || n.includes('sprint') || n.includes('power') || n.includes('plyometric') || n.includes('plyo')) return 'quads_explosive';
    if (n.includes('wall sit') || n.includes('wall squat') || n.includes('isometric')) return 'quads_isometric';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr')) return 'quads_stretch';
    if (n.includes('clean') || n.includes('snatch') || n.includes('thruster')) return 'quads_explosive';
    return 'quads_squat';
  }

  // === HAMSTRINGS ===
  if (pm.includes('hamstrings')) {
    if (n.includes('curl') || n.includes('leg curl')) {
      if (n.includes('seated')) return 'hams_seated_curl';
      if (n.includes('lying') || n.includes('prone')) return 'hams_lying_curl';
      if (n.includes('standing')) return 'hams_standing_curl';
      return 'hams_curl';
    }
    if (n.includes('deadlift') || n.includes('dead lift')) {
      if (n.includes('romanian') || n.includes('rdl') || n.includes('stiff') || n.includes('straight')) return 'hams_rdl';
      if (n.includes('single') || n.includes('one leg') || n.includes('one-leg')) return 'hams_unilateral';
      return 'hams_rdl';
    }
    if (n.includes('good morning') || n.includes('goodmorning')) return 'hams_rdl';
    if (n.includes('glute ham') || n.includes('glute-ham') || n.includes('ghr') || n.includes('nordic') || n.includes('natural')) return 'hams_glute_ham';
    if (n.includes('swing') || n.includes('clean') || n.includes('snatch') || n.includes('hang')) return 'hams_power';
    if (n.includes('bridge') || n.includes('hip thrust') || n.includes('thrust')) return 'hams_hip_extension';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr')) return 'hams_stretch';
    return 'hams_curl';
  }

  // === GLUTES ===
  if (pm.includes('glutes')) {
    if (n.includes('hip thrust') || n.includes('thrust') || n.includes('bridge') || n.includes('glute bridge')) return 'glutes_thrust';
    if (n.includes('squat') || n.includes('sumo')) return 'glutes_squat';
    if (n.includes('lunge') || n.includes('step') || n.includes('split') || n.includes('bulgarian')) return 'glutes_unilateral';
    if (n.includes('kickback') || n.includes('donkey') || n.includes('cable kick') || n.includes('pull through') || n.includes('pull-through')) return 'glutes_kickback';
    if (n.includes('abduct') || n.includes('clam') || n.includes('fire hydrant') || n.includes('band walk') || n.includes('lateral walk')) return 'glutes_medius';
    if (n.includes('deadlift') || n.includes('rdl') || n.includes('romanian')) return 'glutes_hinge';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr') || n.includes('pigeon') || n.includes('knee')) return 'glutes_stretch';
    return 'glutes_thrust';
  }

  // === CALVES ===
  if (pm.includes('calves')) {
    if (n.includes('seated') || n.includes('sitting')) return 'calves_soleus';
    if (n.includes('standing') || n.includes('donkey') || n.includes('smith') || n.includes('machine')) return 'calves_gastrocnemius';
    if (n.includes('press') || n.includes('leg press')) return 'calves_gastrocnemius';
    if (n.includes('jump') || n.includes('hop') || n.includes('skip') || n.includes('bound') || n.includes('box')) return 'calves_explosive';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr') || n.includes('tibialis') || n.includes('ankle') || n.includes('circle')) return 'calves_stretch';
    return 'calves_gastrocnemius';
  }

  // === ABDOMINALS ===
  if (pm.includes('abdominals')) {
    if (n.includes('crunch') || n.includes('sit-up') || n.includes('sit up') || n.includes('situp')) {
      if (n.includes('reverse')) return 'abs_lower';
      if (n.includes('oblique') || n.includes('cross') || n.includes('twist') || n.includes('bicycle')) return 'abs_obliques';
      if (n.includes('cable') || n.includes('machine') || n.includes('weighted')) return 'abs_upper_weighted';
      if (n.includes('decline')) return 'abs_upper';
      return 'abs_upper';
    }
    if (n.includes('plank') || n.includes('bridge') || n.includes('hollow') || n.includes('dead bug') || n.includes('bird dog')) {
      if (n.includes('side')) return 'abs_obliques';
      return 'abs_core_stability';
    }
    if (n.includes('leg raise') || n.includes('knee raise') || n.includes('hanging') || n.includes('captain') || n.includes('toes to bar') || n.includes('toes-to-bar') || n.includes('l-sit') || n.includes('reverse crunch')) return 'abs_lower';
    if (n.includes('russian twist') || n.includes('woodchop') || n.includes('wood chop') || n.includes('oblique') || n.includes('side bend') || n.includes('windshield') || n.includes('twist') || n.includes('rotation') || n.includes('landmine')) return 'abs_obliques';
    if (n.includes('ab wheel') || n.includes('ab roller') || n.includes('rollout') || n.includes('roll out') || n.includes('dragon flag') || n.includes('pike') || n.includes('inchworm') || n.includes('walkout')) return 'abs_core_stability';
    if (n.includes('v-up') || n.includes('v up') || n.includes('jackknife') || n.includes('toe touch')) return 'abs_full';
    if (n.includes('mountain climber') || n.includes('burpee') || n.includes('flutter') || n.includes('scissor') || n.includes('bicycle') || n.includes('air bike')) return 'abs_lower';
    if (n.includes('pallof') || n.includes('anti-rotation') || n.includes('stir') || n.includes('farmer')) return 'abs_core_stability';
    if (n.includes('windmill') || n.includes('turkish') || n.includes('get-up') || n.includes('get up')) return 'abs_core_stability';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr')) return 'abs_stretch';
    if (n.includes('cable') || n.includes('weighted') || n.includes('machine')) return 'abs_upper_weighted';
    return 'abs_upper';
  }

  // === ADDUCTORS ===
  if (pm.includes('adductors')) {
    if (n.includes('machine') || n.includes('hip adduction')) return 'adductors_machine';
    if (n.includes('sumo') || n.includes('wide') || n.includes('plie')) return 'adductors_compound';
    if (n.includes('ball') || n.includes('squeeze') || n.includes('copenhagen')) return 'adductors_isometric';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr') || n.includes('groin')) return 'adductors_stretch';
    return 'adductors_stretch';
  }

  // === ABDUCTORS ===
  if (pm.includes('abductors')) {
    if (n.includes('machine') || n.includes('hip abduction')) return 'abductors_machine';
    if (n.includes('band') || n.includes('lateral walk') || n.includes('monster walk') || n.includes('clam')) return 'abductors_band';
    if (n.includes('fire hydrant') || n.includes('lying') || n.includes('side lying')) return 'abductors_floor';
    if (cat === 'stretching' || n.includes('stretch') || n.includes('foam') || n.includes('smr') || n.includes('it band') || n.includes('iliotibial')) return 'abductors_stretch';
    return 'abductors_floor';
  }

  // === NECK ===
  if (pm.includes('neck')) {
    if (n.includes('flexion') || n.includes('front')) return 'neck_flexion';
    if (n.includes('extension') || n.includes('rear') || n.includes('back')) return 'neck_extension';
    if (n.includes('lateral') || n.includes('side')) return 'neck_lateral';
    if (n.includes('rotation') || n.includes('circle')) return 'neck_rotation';
    return 'neck_flexion';
  }

  // Fallback
  return (pm[0] || 'unknown') + '_general';
}

// ============================================================
// BUILD SUB-TARGET GROUPS
// ============================================================
const subTargetGroups = {};
data.forEach(ex => {
  const st = classifySubTarget(ex);
  ex._subTarget = st;
  if (!subTargetGroups[st]) subTargetGroups[st] = [];
  subTargetGroups[st].push(ex);
});

// Print group stats
console.log('Sub-target groups:');
Object.keys(subTargetGroups).sort().forEach(k => {
  console.log(`  ${k}: ${subTargetGroups[k].length} exercises`);
});

// ============================================================
// DEFINE FALLBACK CHAINS (if sub-target group is too small)
// ============================================================
const fallbackChains = {
  'upper_chest': ['mid_chest', 'lower_chest'],
  'lower_chest': ['mid_chest', 'upper_chest'],
  'mid_chest': ['upper_chest', 'lower_chest'],
  'inner_chest': ['mid_chest', 'upper_chest'],
  'chest_pullover': ['lats_stretch', 'mid_chest'],
  'lateral_delt': ['front_delt', 'overhead_press'],
  'front_delt': ['overhead_press', 'lateral_delt'],
  'rear_delt': ['traps_lower', 'mid_back_row'],
  'overhead_press': ['front_delt', 'lateral_delt'],
  'rotator_cuff': ['rear_delt', 'shoulder_stretch'],
  'shoulder_stretch': ['rotator_cuff', 'rear_delt'],
  'lats_width': ['lats_lower', 'lats_stretch'],
  'lats_lower': ['lats_width', 'lats_thickness'],
  'lats_thickness': ['mid_back_row', 'lats_lower'],
  'lats_stretch': ['chest_pullover', 'lats_width'],
  'mid_back_row': ['lats_thickness', 'mid_back_pull'],
  'mid_back_pull': ['mid_back_row', 'lats_width'],
  'mid_back_bodyweight': ['mid_back_row', 'mid_back_pull'],
  'mid_back_stretch': ['lower_back_stretch', 'mid_back_row'],
  'lower_back_deadlift': ['lower_back_hinge', 'lower_back_extension'],
  'lower_back_extension': ['lower_back_hinge', 'lower_back_deadlift'],
  'lower_back_hinge': ['lower_back_extension', 'lower_back_deadlift'],
  'lower_back_stretch': ['mid_back_stretch', 'lower_back_extension'],
  'lower_back_power': ['lower_back_deadlift', 'lower_back_hinge'],
  'traps_upper': ['traps_mid', 'traps_power'],
  'traps_mid': ['traps_upper', 'mid_back_row'],
  'traps_lower': ['rear_delt', 'traps_mid'],
  'traps_power': ['traps_upper', 'lower_back_power'],
  'biceps_general': ['biceps_short_head', 'biceps_long_head'],
  'biceps_short_head': ['biceps_general', 'biceps_long_head'],
  'biceps_long_head': ['biceps_general', 'biceps_short_head'],
  'biceps_brachialis': ['biceps_brachioradialis', 'biceps_general'],
  'biceps_brachioradialis': ['biceps_brachialis', 'forearms_flexors'],
  'biceps_stretch': ['biceps_general'],
  'triceps_long_head': ['triceps_general', 'triceps_lateral_head'],
  'triceps_lateral_head': ['triceps_general', 'triceps_medial_head'],
  'triceps_medial_head': ['triceps_general', 'triceps_lateral_head'],
  'triceps_general': ['triceps_long_head', 'triceps_lateral_head'],
  'triceps_stretch': ['triceps_general'],
  'forearms_flexors': ['forearms_grip', 'forearms_extensors'],
  'forearms_extensors': ['forearms_flexors', 'forearms_grip'],
  'forearms_grip': ['forearms_flexors', 'forearms_extensors'],
  'forearms_rotation': ['forearms_flexors', 'forearms_extensors'],
  'forearms_stretch': ['forearms_flexors'],
  'quads_squat': ['quads_front_squat', 'quads_unilateral'],
  'quads_front_squat': ['quads_squat', 'quads_rectus_femoris'],
  'quads_unilateral': ['quads_squat', 'quads_front_squat'],
  'quads_rectus_femoris': ['quads_front_squat', 'quads_unilateral'],
  'quads_explosive': ['quads_squat', 'quads_unilateral'],
  'quads_isometric': ['quads_squat', 'quads_unilateral'],
  'quads_inner': ['quads_squat', 'quads_front_squat'],
  'quads_stretch': ['hams_stretch', 'quads_squat'],
  'hams_curl': ['hams_lying_curl', 'hams_seated_curl'],
  'hams_lying_curl': ['hams_curl', 'hams_seated_curl'],
  'hams_seated_curl': ['hams_curl', 'hams_lying_curl'],
  'hams_standing_curl': ['hams_curl', 'hams_lying_curl'],
  'hams_rdl': ['hams_glute_ham', 'hams_hip_extension'],
  'hams_glute_ham': ['hams_rdl', 'hams_hip_extension'],
  'hams_unilateral': ['hams_rdl', 'hams_curl'],
  'hams_power': ['hams_rdl', 'lower_back_power'],
  'hams_hip_extension': ['hams_rdl', 'glutes_thrust'],
  'hams_stretch': ['quads_stretch', 'hams_rdl'],
  'glutes_thrust': ['glutes_kickback', 'glutes_squat'],
  'glutes_squat': ['glutes_thrust', 'glutes_unilateral'],
  'glutes_unilateral': ['glutes_squat', 'glutes_kickback'],
  'glutes_kickback': ['glutes_thrust', 'glutes_unilateral'],
  'glutes_medius': ['abductors_floor', 'glutes_kickback'],
  'glutes_hinge': ['hams_rdl', 'glutes_thrust'],
  'glutes_stretch': ['hams_stretch', 'glutes_thrust'],
  'calves_gastrocnemius': ['calves_soleus', 'calves_explosive'],
  'calves_soleus': ['calves_gastrocnemius', 'calves_explosive'],
  'calves_explosive': ['calves_gastrocnemius', 'calves_soleus'],
  'calves_stretch': ['calves_gastrocnemius', 'calves_soleus'],
  'abs_upper': ['abs_upper_weighted', 'abs_full'],
  'abs_upper_weighted': ['abs_upper', 'abs_full'],
  'abs_lower': ['abs_full', 'abs_upper'],
  'abs_obliques': ['abs_core_stability', 'abs_full'],
  'abs_core_stability': ['abs_obliques', 'abs_full'],
  'abs_full': ['abs_upper', 'abs_lower'],
  'abs_stretch': ['abs_core_stability'],
  'adductors_machine': ['adductors_compound', 'adductors_isometric'],
  'adductors_compound': ['adductors_machine', 'adductors_isometric'],
  'adductors_isometric': ['adductors_machine', 'adductors_compound'],
  'adductors_stretch': ['adductors_machine', 'adductors_compound'],
  'abductors_machine': ['abductors_band', 'abductors_floor'],
  'abductors_band': ['abductors_floor', 'abductors_machine'],
  'abductors_floor': ['abductors_band', 'abductors_machine'],
  'abductors_stretch': ['abductors_floor', 'abductors_band'],
  'neck_flexion': ['neck_extension', 'neck_lateral'],
  'neck_extension': ['neck_flexion', 'neck_lateral'],
  'neck_lateral': ['neck_flexion', 'neck_extension'],
  'neck_rotation': ['neck_lateral', 'neck_flexion'],
};

// ============================================================
// ASSIGN REPLACEMENTS
// For each exercise, pick up to 4 alternatives from the same
// sub-target group (excluding self). If not enough, use fallback.
// ============================================================
const MAX_REPLACEMENTS = 4;

data.forEach(ex => {
  const st = ex._subTarget;
  const candidates = [];

  // First: same sub-target, different exercise
  const sameGroup = (subTargetGroups[st] || []).filter(c => c.name !== ex.name);

  // Sort candidates: prefer same category (strength/stretching), then same equipment
  sameGroup.sort((a, b) => {
    const aCatMatch = a.category === ex.category ? 0 : 1;
    const bCatMatch = b.category === ex.category ? 0 : 1;
    if (aCatMatch !== bCatMatch) return aCatMatch - bCatMatch;
    const aEqMatch = a.equipment === ex.equipment ? 0 : 1;
    const bEqMatch = b.equipment === ex.equipment ? 0 : 1;
    return aEqMatch - bEqMatch;
  });

  candidates.push(...sameGroup);

  // Fallback if not enough
  if (candidates.length < MAX_REPLACEMENTS) {
    const chain = fallbackChains[st] || [];
    for (const fb of chain) {
      if (candidates.length >= MAX_REPLACEMENTS) break;
      const fbGroup = (subTargetGroups[fb] || []).filter(c =>
        c.name !== ex.name && !candidates.find(cc => cc.name === c.name)
      );
      fbGroup.sort((a, b) => {
        const aCatMatch = a.category === ex.category ? 0 : 1;
        const bCatMatch = b.category === ex.category ? 0 : 1;
        return aCatMatch - bCatMatch;
      });
      candidates.push(...fbGroup);
    }
  }

  // Take top N
  ex.replacementExercises = candidates.slice(0, MAX_REPLACEMENTS).map(c => c.name);

  // Clean up internal field
  delete ex._subTarget;
});

// Write output
fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');

console.log('\nDone! Added replacementExercises to all', data.length, 'exercises');

// Spot check some exercises
const spotChecks = [
  'Barbell Bench Press - Medium Grip',
  'Incline Dumbbell Press',
  'Dumbbell Flyes',
  'Barbell Deadlift',
  'Pullups',
  'Barbell Curl',
  'Dips - Triceps Version',
  'Standing Calf Raises',
  'Barbell Squat',
  'Face Pull',
  'Side Lateral Raise',
  'Preacher Curl',
  'Hammer Curls',
  'Cable Crossover',
  'Bent Over Barbell Row',
];
console.log('\n=== SPOT CHECKS ===');
spotChecks.forEach(name => {
  const ex = data.find(e => e.name === name);
  if (ex) {
    console.log(`\n${ex.name} (${ex.primaryMuscles.join(', ')})`);
    console.log(`  Replacements: ${ex.replacementExercises.join(', ')}`);
  }
});
