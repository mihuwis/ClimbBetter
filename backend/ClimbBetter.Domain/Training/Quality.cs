namespace ClimbBetter.Domain.Training;

public class Quality
{
    public int Id { get; set; }
    public string Name { get; set; } = default!;
    public string Code { get; set; } = default!;
    public decimal Multiplier { get; set; }
}