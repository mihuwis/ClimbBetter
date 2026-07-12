class ClimbProfilePreset {
  const ClimbProfilePreset({
    required this.name,
    required this.description,
    required this.defaultEdl,
  });

  final String name;
  final String description;
  final int defaultEdl;
}

const climbProfilePresets = [
  ClimbProfilePreset(
    name: 'Bald',
    description: 'Klasyczny bald 4-10 ruchow.',
    defaultEdl: 4,
  ),
  ClimbProfilePreset(
    name: 'Krotka droga / baldowa',
    description: '4-12 ruchow, w sumie bald z lina.',
    defaultEdl: 8,
  ),
  ClimbProfilePreset(
    name: 'Krotka droga ciagowa',
    description: '8-15 ruchow ciagowe.',
    defaultEdl: 10,
  ),
  ClimbProfilePreset(
    name: 'Srednia ciagowa',
    description: '15-40 ruchow bez wyraznego cruxa.',
    defaultEdl: 28,
  ),
  ClimbProfilePreset(
    name: 'Srednia cruxowa',
    description: 'Wyrazny crux, odpoczynki, latwiejsze sekcje.',
    defaultEdl: 15,
  ),
  ClimbProfilePreset(
    name: 'Dluga ciagowa',
    description: 'Dluga, ponad 35-40 ruchow.',
    defaultEdl: 40,
  ),
  ClimbProfilePreset(
    name: 'Dluga cruxowa',
    description: 'Wyrazny crux, odpoczynki, latwiejsze sekcje.',
    defaultEdl: 22,
  ),
  ClimbProfilePreset(
    name: 'Obwod',
    description: 'Treningowy obwod na panelu.',
    defaultEdl: 20,
  ),
  ClimbProfilePreset(
    name: 'Rozgrzewka',
    description: 'Rozgrzewka.',
    defaultEdl: 0,
  ),
];
