namespace ClimbBetter.Domain.Training;

public class TrainingSession
{
public Guid Id { get; set; }

public DateTime Date { get; set; }

public Guid? LocationId { get; set; }
public Location? Location { get; set; }

public string? Goal { get; set; }
public string? Method { get; set; }

public Guid? MesocycleId { get; set; }

public ICollection<TrainingEntry> Entries { get; set; } = new List<TrainingEntry>();
}