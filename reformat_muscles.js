const fs = require('fs');
const data = JSON.parse(fs.readFileSync('app/src/main/res/raw/exercises.json', 'utf8'));

// The sub-target mapping functions from add_subtargets.js are already applied.
// We need to re-derive which sub-targets belong to which muscle.
// We'll re-use the same logic but output grouped format.

// We'll load the existing add_subtargets.js functions by requiring it indirectly.
// Instead, let's just use the existing data: each exercise has primaryMuscles, secondaryMuscles,
// primarySubTargets, secondarySubTargets. We need to map sub-targets back to their parent muscle.

// Sub-target to parent muscle mapping
const subTargetToMuscle = {
  // chest
  'upper chest': 'chest', 'mid chest': 'chest', 'lower chest': 'chest', 'inner chest': 'chest', 'outer chest': 'chest',
  // shoulders
  'front delts': 'shoulders', 'side delts': 'shoulders', 'rear delts': 'shoulders', 'rotator cuff': 'shoulders',
  // biceps
  'long head': null, // ambiguous - could be biceps or triceps
  'short head': 'biceps',
  'brachialis': 'biceps', 'brachioradialis': null, // could be biceps or forearms
  // triceps
  'lateral head': 'triceps', 'medial head': 'triceps',
  // quadriceps
  'vastus lateralis': 'quadriceps', 'vastus medialis': 'quadriceps', 'rectus femoris': 'quadriceps',
  // hamstrings
  'biceps femoris': 'hamstrings', 'semitendinosus': 'hamstrings', 'semimembranosus': 'hamstrings',
  // glutes
  'gluteus maximus': 'glutes', 'gluteus medius': null, 'gluteus minimus': null, // could be glutes or abductors
  // calves
  'gastrocnemius': 'calves', 'soleus': 'calves', 'tibialis anterior': 'calves',
  // abdominals
  'upper abs': 'abdominals', 'lower abs': 'abdominals', 'obliques': 'abdominals', 'transverse abdominis': 'abdominals',
  // lats
  'upper lats': 'lats', 'lower lats': 'lats',
  // middle back
  'rhomboids': 'middle back', 'mid traps': null, // could be middle back or traps
  'erector spinae': null, // could be middle back or lower back
  // traps
  'upper traps': 'traps', 'lower traps': 'traps',
  // lower back (erector spinae handled contextually)
  // forearms
  'wrist flexors': 'forearms', 'wrist extensors': 'forearms',
  // adductors
  'adductor magnus': 'adductors', 'adductor longus': 'adductors', 'gracilis': 'adductors',
  // abductors
  'tensor fasciae latae': 'abductors',
  // neck
  'sternocleidomastoid': 'neck', 'splenius': 'neck', 'scalenes': 'neck',
};

// For ambiguous sub-targets, determine parent based on which muscles are in the list
function assignSubTargetsToMuscles(muscles, subTargets) {
  const muscleSet = new Set(muscles);
  const result = {};
  muscles.forEach(m => { result[m] = []; });

  subTargets.forEach(sub => {
    const directParent = subTargetToMuscle[sub];
    if (directParent && muscleSet.has(directParent)) {
      result[directParent].push(sub);
    } else {
      // Handle ambiguous cases
      if (sub === 'long head') {
        if (muscleSet.has('biceps')) result['biceps'].push(sub);
        else if (muscleSet.has('triceps')) result['triceps'].push(sub);
      } else if (sub === 'brachioradialis') {
        if (muscleSet.has('forearms')) result['forearms'].push(sub);
        else if (muscleSet.has('biceps')) result['biceps'].push(sub);
      } else if (sub === 'gluteus medius' || sub === 'gluteus minimus') {
        if (muscleSet.has('abductors')) result['abductors'].push(sub);
        else if (muscleSet.has('glutes')) result['glutes'].push(sub);
      } else if (sub === 'mid traps') {
        if (muscleSet.has('traps')) result['traps'].push(sub);
        else if (muscleSet.has('middle back')) result['middle back'].push(sub);
      } else if (sub === 'erector spinae') {
        if (muscleSet.has('lower back')) result['lower back'].push(sub);
        else if (muscleSet.has('middle back')) result['middle back'].push(sub);
      } else if (sub === 'upper traps') {
        if (muscleSet.has('traps')) result['traps'].push(sub);
        else if (muscleSet.has('neck')) result['neck'].push(sub);
        else if (muscleSet.has('shoulders')) result['shoulders'].push(sub);
      } else if (sub === 'lower traps') {
        if (muscleSet.has('traps')) result['traps'].push(sub);
        else if (muscleSet.has('middle back')) result['middle back'].push(sub);
      } else {
        // Fallback: assign to first muscle
        if (muscles.length > 0) result[muscles[0]].push(sub);
      }
    }
  });

  return muscles.map(m => ({
    target: m,
    subTargets: result[m]
  }));
}

// Process all exercises
data.forEach(exercise => {
  const primaryGrouped = assignSubTargetsToMuscles(
    exercise.primaryMuscles,
    exercise.primarySubTargets || []
  );
  const secondaryGrouped = assignSubTargetsToMuscles(
    exercise.secondaryMuscles || [],
    exercise.secondarySubTargets || []
  );

  exercise.primaryMuscles = primaryGrouped;
  exercise.secondaryMuscles = secondaryGrouped;

  // Remove old flat fields
  delete exercise.primarySubTargets;
  delete exercise.secondarySubTargets;
});

fs.writeFileSync('app/src/main/res/raw/exercises.json', JSON.stringify(data, null, 2), 'utf8');

console.log('Done. Reformatted', data.length, 'exercises');

// Show samples
const samples = ['Barbell Bench Press - Medium Grip', 'Barbell Incline Bench Press - Medium Grip', 'Arnold Dumbbell Press', 'Barbell Curl', 'Barbell Deadlift', 'Barbell Full Squat'];
samples.forEach(name => {
  const ex = data.find(e => e.name === name);
  if (ex) {
    console.log(`\n${ex.name}:`);
    console.log('  Primary:', JSON.stringify(ex.primaryMuscles));
    console.log('  Secondary:', JSON.stringify(ex.secondaryMuscles));
  }
});
