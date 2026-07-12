class AscentStylePreset {
  const AscentStylePreset({
    required this.name,
    required this.multiplier,
    required this.description,
  });

  final String name;
  final double multiplier;
  final String description;
}

const ascentStylePresets = [
  AscentStylePreset(
    name: 'OS/Flash',
    multiplier: 1.5,
    description: 'Przejscie od razu albo bardzo szybkie.',
  ),
  AscentStylePreset(
    name: 'RP slaba znajomosc',
    multiplier: 1.3,
    description: 'Pelny RP po slabym rozeznaniu.',
  ),
  AscentStylePreset(
    name: 'RP normalny',
    multiplier: 1,
    description: 'RP / attempt RP okolo 5-12 prob w sezonie.',
  ),
  AscentStylePreset(
    name: 'RP staly',
    multiplier: 0.75,
    description: 'Proby powtarzane, znana droga/bald.',
  ),
  AscentStylePreset(
    name: 'Rozgrzewka',
    multiplier: 0,
    description: 'Nie liczy sie do punktow, liczy sie do ruchow.',
  ),
  AscentStylePreset(
    name: 'Attempt OS/FL',
    multiplier: 1.5,
    description: 'Proba od razu albo bardzo szybka.',
  ),
  AscentStylePreset(
    name: 'Attempt slaba znajomosc',
    multiplier: 1.2,
    description: 'Proba po slabym rozeznaniu.',
  ),
  AscentStylePreset(
    name: 'Attempt normalny',
    multiplier: 0.9,
    description: 'Proba okolo 5-12 prob w sezonie.',
  ),
  AscentStylePreset(
    name: 'Attempt staly',
    multiplier: 0.5,
    description: 'Regularne probowanie tego samego problemu.',
  ),
];

final defaultDoneStyle = ascentStylePresets[2];
final defaultAttemptStyle = ascentStylePresets[7];
