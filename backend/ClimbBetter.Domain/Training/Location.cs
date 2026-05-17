namespace ClimbBetter.Domain.Training;

public class Location
{
    public Guid Id { get; set; }

    public string Name { get; set; } = default!;

    public string Continent { get; set; } = default!;

    public string Country { get; set; } = default!;

    public string? Area { get; set; }

    public string? Sector { get; set; }

    public string? SubArea { get; set; }

    public LocationType Type { get; set; }

    public Guid? OwnerId { get; set; }

    public bool IsPublic { get; set; }
}


public enum LocationType
{
    ArtificialWall = 1,
    BoulderGym = 2,
    SportCrag = 3,
    BoulderArea = 4
}