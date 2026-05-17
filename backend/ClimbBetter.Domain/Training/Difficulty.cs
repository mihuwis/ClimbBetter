namespace ClimbBetter.Domain.Training;

public class Difficulty
{
    public int Id { get; set; }
    public string Name { get; set; } = default!;
    public int Points { get; set; }
}