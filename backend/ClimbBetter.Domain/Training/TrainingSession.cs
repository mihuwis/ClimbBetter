namespace ClimbBetter.Domain.Training;

public class TrainingSession
{
    public Guid Id {get; set; }

    public DateTime Date { get; set; }

    public TimeSpan Duration { get; set; }
}